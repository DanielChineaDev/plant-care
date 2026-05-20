/**
 * Cloud Functions de PlantCare.
 *
 * Triggers:
 *   onChatMessageCreated    -> conversations/{cid}/messages/{mid}
 *   onPostLiked             -> communities/{cid}/posts/{pid}/likes/{uid}
 *   onPostCommentCreated    -> communities/{cid}/posts/{pid}/comments/{cmid}
 *   onCommunityMemberJoined -> communities/{cid}/members/{uid}
 *
 * Cada trigger:
 *   1) Calcula el destinatario de la notificacion (autor del post, creador
 *      de la comunidad, etc.) y un payload con metadata.
 *   2) Escribe un doc en notifications/{recipientUid}/items/{autoId} para
 *      que el cliente lo lea en el "centro de notificaciones" dentro de la
 *      app.
 *   3) Manda FCM al recipient si tiene tokens registrados.
 *
 * Requisitos:
 *   - Proyecto Firebase en plan Blaze.
 *   - Despliegue: `firebase deploy --only functions`
 */

const {onDocumentCreated, onDocumentDeleted} = require("firebase-functions/v2/firestore");
const {onRequest} = require("firebase-functions/v2/https");
const {logger} = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

const db = admin.firestore();
const messaging = admin.messaging();

const REGION = "europe-west1";

// Catalogo de comunidades semilla con descripciones tematicas ricas.
// Compartido por seedCommunities (crea) y updateCommunityDescriptions
// (actualiza solo la descripcion de las existentes).
const COMMUNITY_SEEDS = [
  {
    name: "Suculentas y cactus",
    emoji: "🌵",
    description: "El club de las plantas que aguantan de todo. Comparte tus " +
      "echeverias, haworthias, aloes y cactus, resuelve dudas sobre riego " +
      "por inmersion, sustratos drenantes, etiolado por falta de luz y " +
      "como sacar hijuelos. Ideal si empiezas: son las plantas mas " +
      "agradecidas.",
  },
  {
    name: "Orquidiarios",
    emoji: "🌸",
    description: "Para enamorados de las orquideas. Phalaenopsis, cymbidium, " +
      "dendrobium y mas: hablamos de refloracion, raices aereas, riego por " +
      "inmersion, corteza vs musgo y como recuperar una orquidea que parece " +
      "muerta. Sube fotos de tus varas en flor.",
  },
  {
    name: "Bonsais",
    emoji: "🌳",
    description: "El arte de cultivar arboles en miniatura. Tecnicas de poda, " +
      "alambrado, pinzado y trasplante; especies para principiantes (ficus, " +
      "olmo chino) y para expertos. Comparte la evolucion de tus arboles a " +
      "lo largo de las estaciones.",
  },
  {
    name: "Huerto urbano",
    emoji: "🥬",
    description: "Cultiva tu propia comida en balcones, terrazas y alfeizares. " +
      "Tomateras en maceta, lechugas, fresas, pimientos y hierbas. Hablamos " +
      "de calendario de siembra, compost casero, polinizacion manual y como " +
      "aprovechar poco espacio al maximo.",
  },
  {
    name: "Plantas de interior",
    emoji: "🪴",
    description: "Convierte tu casa en una jungla. Pothos, monsteras, ficus, " +
      "calatheas, potos y filodendros. Resolvemos hojas amarillas, falta de " +
      "humedad, ubicacion segun la luz de cada ventana y como mantenerlas " +
      "vivas en invierno con la calefaccion.",
  },
  {
    name: "Aromaticas y medicinales",
    emoji: "🌿",
    description: "Albahaca, menta, romero, lavanda, tomillo, aloe vera... " +
      "Plantas que se comen, se huelen y se usan. Comparte recetas, infusiones, " +
      "como secar y conservar hierbas y trucos para que tu albahaca no se " +
      "espigue en cuanto llega el calor.",
  },
  {
    name: "Plagas y enfermedades",
    emoji: "🪲",
    description: "SOS para tus plantas. Sube una foto del problema y la " +
      "comunidad te ayuda a identificar cochinilla, arana roja, mosca blanca, " +
      "oidio, mildiu o pudricion de raiz. Remedios caseros y ecologicos, " +
      "jabon potasico, aceite de neem y prevencion.",
  },
  {
    name: "Acuaponia e hidroponia",
    emoji: "💧",
    description: "Cultivo sin tierra. Sistemas NFT, DWC, mechas y aeroponia; " +
      "control de pH y EC, soluciones nutritivas, iluminacion LED y " +
      "acuaponia combinando peces y plantas. Para los que disfrutan tanto la " +
      "jardineria como cacharrear con sistemas.",
  },
  {
    name: "Jardin exterior",
    emoji: "🌻",
    description: "Todo lo que vive a la intemperie: parterres, setos, rosales, " +
      "girasoles, arbustos y flores de temporada. Diseno de jardin, plantas " +
      "segun el clima, cesped, riego automatico y como tener color todo el " +
      "ano. Comparte tu rincon verde.",
  },
  {
    name: "Propagacion y esquejes",
    emoji: "🌱",
    description: "Multiplica tus plantas gratis. Esquejes de tallo y hoja, " +
      "acodo aereo, division de mata, germinacion de semillas y enraizado en " +
      "agua vs sustrato. Comparte tus exitos (y fracasos) e intercambia " +
      "esquejes con otros miembros.",
  },
];

// ============================================================================
// Actualiza SOLO la descripcion de las comunidades semilla existentes
// (match por nameLower). No crea nuevas. Util para refrescar textos sin
// tocar memberCount ni otros campos.
// ============================================================================
exports.updateCommunityDescriptions = onRequest(
  {region: REGION, timeoutSeconds: 120},
  async (req, res) => {
    try {
      let updated = 0;
      let notFound = 0;
      for (const seed of COMMUNITY_SEEDS) {
        const nameLower = seed.name.toLowerCase();
        const snap = await db.collection("communities")
          .where("nameLower", "==", nameLower)
          .limit(1)
          .get();
        if (snap.empty) {
          notFound++;
          continue;
        }
        await snap.docs[0].ref.update({description: seed.description});
        updated++;
      }
      res.status(200).json({ok: true, updated, notFound});
    } catch (err) {
      logger.error("updateCommunityDescriptions fallo", err);
      res.status(500).json({ok: false, error: String(err)});
    }
  },
);

// ============================================================================
// Chat 1-a-1: notificacion push y deep link al chat.
// ============================================================================
exports.onChatMessageCreated = onDocumentCreated(
  {
    document: "conversations/{conversationId}/messages/{messageId}",
    region: REGION,
  },
  async (event) => {
    const snap = event.data;
    if (!snap) return;
    const message = snap.data();
    const {conversationId} = event.params;

    const senderUid = message.senderUid;
    const text = message.text || "";
    if (!senderUid || !text) return;

    const convSnap = await db.collection("conversations").doc(conversationId).get();
    if (!convSnap.exists) return;
    const participants = convSnap.data().participants || [];
    const recipients = participants.filter((uid) => uid !== senderUid);
    if (recipients.length === 0) return;

    const senderName = await loadDisplayName(senderUid, "Nuevo mensaje");

    await Promise.all(
      recipients.map((recipientUid) =>
        sendChatPush({recipientUid, senderUid, senderName, text}),
      ),
    );
  },
);

// ============================================================================
// Like en post: notificacion al autor del post.
// ============================================================================
exports.onPostLiked = onDocumentCreated(
  {
    document: "communities/{communityId}/posts/{postId}/likes/{likerUid}",
    region: REGION,
  },
  async (event) => {
    const {communityId, postId, likerUid} = event.params;

    const postSnap = await db
      .collection("communities").doc(communityId)
      .collection("posts").doc(postId)
      .get();
    if (!postSnap.exists) return;
    const authorUid = postSnap.data().authorUid;
    if (!authorUid || authorUid === likerUid) return;

    const likerName = await loadDisplayName(likerUid, "Alguien");
    const postText = (postSnap.data().text || "").slice(0, 80);

    // Karma +1 al autor del post por like recibido.
    await incrementKarma(authorUid, 1);

    await writeNotification(authorUid, {
      type: "post_like",
      fromUid: likerUid,
      fromName: likerName,
      communityId,
      postId,
      preview: postText,
    });

    await sendActivityPush({
      recipientUid: authorUid,
      title: `${likerName} le ha dado like a tu publicacion`,
      body: postText || "Toca para verla",
      data: {
        type: "post",
        communityId,
        postId,
      },
    });
  },
);

// ============================================================================
// Like retirado: -1 karma al autor del post.
// ============================================================================
exports.onPostUnliked = onDocumentDeleted(
  {
    document: "communities/{communityId}/posts/{postId}/likes/{likerUid}",
    region: REGION,
  },
  async (event) => {
    const {communityId, postId} = event.params;
    const postSnap = await db
      .collection("communities").doc(communityId)
      .collection("posts").doc(postId)
      .get();
    if (!postSnap.exists) return;
    const authorUid = postSnap.data().authorUid;
    if (!authorUid) return;
    await incrementKarma(authorUid, -1);
  },
);

// ============================================================================
// Comentario en post: notificacion al autor del post.
// ============================================================================
exports.onPostCommentCreated = onDocumentCreated(
  {
    document: "communities/{communityId}/posts/{postId}/comments/{commentId}",
    region: REGION,
  },
  async (event) => {
    const snap = event.data;
    if (!snap) return;
    const comment = snap.data();
    const {communityId, postId} = event.params;

    const commenterUid = comment.authorUid;
    if (!commenterUid) return;

    const postSnap = await db
      .collection("communities").doc(communityId)
      .collection("posts").doc(postId)
      .get();
    if (!postSnap.exists) return;
    const authorUid = postSnap.data().authorUid;
    if (!authorUid || authorUid === commenterUid) return;

    const commenterName = comment.authorName || (await loadDisplayName(commenterUid, "Alguien"));
    const commentText = (comment.text || "").slice(0, 120);

    await writeNotification(authorUid, {
      type: "post_comment",
      fromUid: commenterUid,
      fromName: commenterName,
      communityId,
      postId,
      preview: commentText,
    });

    await sendActivityPush({
      recipientUid: authorUid,
      title: `${commenterName} ha comentado tu publicacion`,
      body: commentText || "Toca para verlo",
      data: {
        type: "post",
        communityId,
        postId,
      },
    });
  },
);

// ============================================================================
// Nuevo miembro en una comunidad: notificacion al creador de la comunidad.
// ============================================================================
exports.onCommunityMemberJoined = onDocumentCreated(
  {
    document: "communities/{communityId}/members/{memberUid}",
    region: REGION,
  },
  async (event) => {
    const {communityId, memberUid} = event.params;

    const commSnap = await db.collection("communities").doc(communityId).get();
    if (!commSnap.exists) return;
    const creatorUid = commSnap.data().createdBy;
    const communityName = commSnap.data().name || "tu comunidad";
    if (!creatorUid || creatorUid === memberUid) return;

    const memberName = await loadDisplayName(memberUid, "Alguien");

    await writeNotification(creatorUid, {
      type: "community_join",
      fromUid: memberUid,
      fromName: memberName,
      communityId,
      preview: `Se ha unido a ${communityName}`,
    });

    await sendActivityPush({
      recipientUid: creatorUid,
      title: `${memberName} se ha unido a ${communityName}`,
      body: "Toca para ver la comunidad",
      data: {
        type: "community",
        communityId,
      },
    });
  },
);

// ============================================================================
// Helpers
// ============================================================================

// ============================================================================
// Seed inicial de 10 comunidades tematicas. Solo crea las que NO existan
// (key = nombre normalizado). Idempotente. Las comunidades arrancan con
// memberCount = 0 y un createdBy "system" (el doc se crea con Admin SDK
// que salta las reglas; no representa un user real).
// ============================================================================
exports.seedCommunities = onRequest(
  {region: REGION, timeoutSeconds: 120},
  async (req, res) => {
    try {
      const seeds = COMMUNITY_SEEDS;

      let created = 0;
      let skipped = 0;
      for (const seed of seeds) {
        const nameLower = seed.name.toLowerCase();
        const existing = await db.collection("communities")
          .where("nameLower", "==", nameLower)
          .limit(1)
          .get();
        if (!existing.empty) {
          skipped++;
          continue;
        }
        await db.collection("communities").add({
          name: seed.name,
          nameLower,
          description: seed.description,
          emoji: seed.emoji,
          createdBy: "system",
          createdAt: admin.firestore.FieldValue.serverTimestamp(),
          memberCount: 0,
          photoUrl: null,
        });
        created++;
      }

      res.status(200).json({ok: true, created, skipped, total: seeds.length});
    } catch (err) {
      logger.error("seedCommunities fallo", err);
      res.status(500).json({ok: false, error: String(err)});
    }
  },
);

// ============================================================================
// Backfill manual de campos lowercase para busqueda case-insensitive.
//
// Cuando se anaden campos nameLower / displayNameLower a comunidades y
// usuarios, los documentos viejos no los tienen y la query global no los
// encuentra. Esta funcion HTTP recorre TODA la coleccion y rellena el
// campo derivado. Idempotente: si ya existe nameLower y coincide, no
// escribe (ahorra writes).
//
// Solo invocable manualmente desde gcloud / Firebase Console / curl
// usando un token de un user admin. Por simplicidad MVP la dejamos
// abierta pero requiere conocer la URL; en produccion conviene
// proteger con Bearer auth + check de isAdmin.
// ============================================================================
exports.backfillLowercaseFields = onRequest(
  {region: REGION, timeoutSeconds: 540},
  async (req, res) => {
    try {
      const communitiesSnap = await db.collection("communities").get();
      let communitiesUpdated = 0;
      const cBatch = db.batch();
      communitiesSnap.docs.forEach((doc) => {
        const data = doc.data();
        const name = data.name;
        if (!name) return;
        const lower = String(name).toLowerCase();
        if (data.nameLower !== lower) {
          cBatch.update(doc.ref, {nameLower: lower});
          communitiesUpdated++;
        }
      });
      if (communitiesUpdated > 0) await cBatch.commit();

      const usersSnap = await db.collection("users").get();
      let usersUpdated = 0;
      const uBatch = db.batch();
      usersSnap.docs.forEach((doc) => {
        const data = doc.data();
        const dn = data.displayName;
        if (!dn) return;
        const lower = String(dn).toLowerCase();
        if (data.displayNameLower !== lower) {
          uBatch.update(doc.ref, {displayNameLower: lower});
          usersUpdated++;
        }
      });
      if (usersUpdated > 0) await uBatch.commit();

      res.status(200).json({
        ok: true,
        communitiesUpdated,
        usersUpdated,
      });
    } catch (err) {
      logger.error("backfill fallo", err);
      res.status(500).json({ok: false, error: String(err)});
    }
  },
);

/**
 * Suma [delta] al campo karma del usuario [uid], con suelo en 0 (el karma
 * nunca puede ser negativo). Usamos una transaccion para leer el valor
 * actual y aplicar el clamp; FieldValue.increment no permite acotar.
 */
async function incrementKarma(uid, delta) {
  try {
    const ref = db.collection("users").doc(uid);
    await db.runTransaction(async (tx) => {
      const snap = await tx.get(ref);
      const current = (snap.exists && typeof snap.data().karma === "number") ?
        snap.data().karma : 0;
      const next = Math.max(0, current + delta);
      tx.set(ref, {karma: next}, {merge: true});
    });
  } catch (err) {
    logger.warn("incrementKarma fallo", {uid, delta, err});
  }
}

async function loadDisplayName(uid, fallback) {
  try {
    const snap = await db.collection("users").doc(uid).get();
    if (snap.exists) {
      const d = snap.data();
      return d.displayName || fallback;
    }
  } catch (err) {
    logger.warn("loadDisplayName fallo", {uid, err});
  }
  return fallback;
}

/**
 * Inserta un doc en notifications/{uid}/items/{autoId}. El cliente observa
 * esta subcoleccion para el centro de notificaciones y el badge.
 */
async function writeNotification(recipientUid, payload) {
  try {
    await db.collection("notifications").doc(recipientUid)
      .collection("items").add({
        ...payload,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        read: false,
      });
  } catch (err) {
    logger.error("writeNotification fallo", {recipientUid, err});
  }
}

async function sendChatPush({recipientUid, senderUid, senderName, text}) {
  await sendMulticast({
    recipientUid,
    notification: {title: senderName, body: text},
    data: {
      type: "chat",
      chatUid: senderUid,
      title: senderName,
      body: text,
    },
    androidChannel: "chat_messages",
  });
}

async function sendActivityPush({recipientUid, title, body, data}) {
  // Sanitize: FCM data values deben ser todo strings.
  const stringifiedData = {};
  Object.keys(data || {}).forEach((k) => {
    stringifiedData[k] = String(data[k]);
  });
  await sendMulticast({
    recipientUid,
    notification: {title, body},
    data: stringifiedData,
    androidChannel: "activity_notifications",
  });
}

async function sendMulticast({recipientUid, notification, data, androidChannel}) {
  const tokensCol = db.collection("users").doc(recipientUid).collection("fcmTokens");
  const tokensSnap = await tokensCol.get();
  if (tokensSnap.empty) return;

  const tokens = tokensSnap.docs.map((d) => d.id);
  const payload = {
    notification,
    data: data || {},
    android: {
      priority: "high",
      notification: {channelId: androidChannel, sound: "default"},
    },
    tokens,
  };

  try {
    const response = await messaging.sendEachForMulticast(payload);
    logger.info("Push enviado", {
      recipientUid,
      channel: androidChannel,
      success: response.successCount,
      failure: response.failureCount,
    });

    const stale = [];
    response.responses.forEach((resp, idx) => {
      if (!resp.success) {
        const code = resp.error && resp.error.code;
        if (
          code === "messaging/invalid-registration-token" ||
          code === "messaging/registration-token-not-registered"
        ) {
          stale.push(tokens[idx]);
        }
      }
    });
    if (stale.length > 0) {
      await Promise.all(stale.map((t) => tokensCol.doc(t).delete().catch(() => {})));
    }
  } catch (err) {
    logger.error("Multicast fallo", {recipientUid, err});
  }
}

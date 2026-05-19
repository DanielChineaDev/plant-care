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

const {onDocumentCreated} = require("firebase-functions/v2/firestore");
const {logger} = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

const db = admin.firestore();
const messaging = admin.messaging();

const REGION = "europe-west1";

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

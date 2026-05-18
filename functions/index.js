/**
 * Cloud Functions de PlantCare.
 *
 * Trigger:
 *   onMessageCreated -> conversations/{cid}/messages/{mid}
 *
 * Flujo:
 *   1. Lee el doc de la conversacion padre y obtiene participants[].
 *   2. Calcula el recipientUid (el participante que NO es el sender).
 *   3. Lee users/{recipientUid}/fcmTokens/* para obtener los tokens
 *      activos en sus dispositivos.
 *   4. Envia un multicast con title=nombre del sender, body=texto del
 *      mensaje y data={type:"chat", chatUid: senderUid} para deep-link.
 *   5. Limpia tokens invalidos (UNREGISTERED / INVALID_ARGUMENT) del
 *      Firestore para no acumular basura.
 *
 * Requisitos:
 *   - Proyecto Firebase en plan Blaze (Cloud Functions de pago segun uso;
 *     el tier gratis cubre ~125K invocaciones/mes, sobra para esto).
 *   - Despliegue: `firebase deploy --only functions`
 */

const {onDocumentCreated} = require("firebase-functions/v2/firestore");
const {logger} = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

const db = admin.firestore();
const messaging = admin.messaging();

exports.onChatMessageCreated = onDocumentCreated(
  {
    document: "conversations/{conversationId}/messages/{messageId}",
    region: "us-central1",
  },
  async (event) => {
    const snap = event.data;
    if (!snap) {
      logger.warn("Sin snapshot en el evento, salgo.");
      return;
    }
    const message = snap.data();
    const {conversationId} = event.params;

    const senderUid = message.senderUid;
    const text = message.text || "";
    if (!senderUid || !text) {
      logger.warn("Mensaje sin senderUid o text, salgo.", {conversationId});
      return;
    }

    // 1. Cargar conversacion padre para obtener participants.
    const convRef = db.collection("conversations").doc(conversationId);
    const convSnap = await convRef.get();
    if (!convSnap.exists) {
      logger.warn("Conversacion padre no existe", {conversationId});
      return;
    }
    const conv = convSnap.data();
    const participants = conv.participants || [];
    const recipients = participants.filter((uid) => uid !== senderUid);
    if (recipients.length === 0) {
      logger.warn("Sin recipients", {conversationId, senderUid});
      return;
    }

    // 2. Cargar perfil del sender para mostrar nombre.
    let senderName = "Nuevo mensaje";
    try {
      const senderSnap = await db.collection("users").doc(senderUid).get();
      if (senderSnap.exists) {
        const senderData = senderSnap.data();
        senderName = senderData.displayName || senderName;
      }
    } catch (err) {
      logger.warn("No pude leer perfil del sender", {senderUid, err});
    }

    // 3. Por cada recipient, leer sus tokens FCM y mandar push.
    await Promise.all(
      recipients.map((recipientUid) =>
        sendToRecipient({recipientUid, senderUid, senderName, text}),
      ),
    );
  },
);

async function sendToRecipient({recipientUid, senderUid, senderName, text}) {
  const tokensCol = db
    .collection("users")
    .doc(recipientUid)
    .collection("fcmTokens");
  const tokensSnap = await tokensCol.get();
  if (tokensSnap.empty) {
    logger.info("Recipient sin tokens FCM", {recipientUid});
    return;
  }

  const tokens = tokensSnap.docs.map((d) => d.id);

  // chatUid en el data payload = el OTRO usuario desde el punto de vista
  // del recipient (es decir, el sender). Eso casa con Routes.chat(uid) en
  // el cliente, que abre el chat con ese uid.
  const messagePayload = {
    notification: {
      title: senderName,
      body: text,
    },
    data: {
      type: "chat",
      chatUid: senderUid,
      title: senderName,
      body: text,
    },
    android: {
      priority: "high",
      notification: {
        channelId: "chat_messages",
        sound: "default",
      },
    },
    tokens,
  };

  try {
    const response = await messaging.sendEachForMulticast(messagePayload);
    logger.info("Push enviado", {
      recipientUid,
      success: response.successCount,
      failure: response.failureCount,
    });

    // 4. Limpiar tokens invalidos.
    const stale = [];
    response.responses.forEach((resp, idx) => {
      if (!resp.success) {
        const code = resp.error && resp.error.code;
        if (
          code === "messaging/invalid-registration-token" ||
          code === "messaging/registration-token-not-registered"
        ) {
          stale.push(tokens[idx]);
        } else {
          logger.warn("Fallo push (no tocamos token)", {token: tokens[idx], code});
        }
      }
    });
    if (stale.length > 0) {
      logger.info("Borrando tokens invalidos", {recipientUid, count: stale.length});
      await Promise.all(
        stale.map((t) => tokensCol.doc(t).delete().catch(() => {})),
      );
    }
  } catch (err) {
    logger.error("Error mandando multicast", {recipientUid, err});
  }
}

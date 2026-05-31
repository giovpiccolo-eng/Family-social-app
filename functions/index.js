/**
 * FamilyNest Cloud Functions.
 *
 * When a new post is created in families/{familyId}/posts/{postId}, notify every
 * other family member's devices via FCM.
 *
 * Deploy with:  firebase deploy --only functions
 */
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

const TYPE_LABELS = {
  IDEA: "💡 a new idea",
  EVENT: "📅 a new event",
  PLAN: "🗺️ a new plan",
  PROJECT: "🛠️ a new project",
  IMAGE: "🖼️ a new photo",
};

exports.notifyFamilyOnNewPost = onDocumentCreated(
  "families/{familyId}/posts/{postId}",
  async (event) => {
    const post = event.data?.data();
    if (!post) return;

    const { familyId, postId } = event.params;
    const db = getFirestore();

    // Look up the family's members.
    const familySnap = await db.collection("families").doc(familyId).get();
    const memberIds = familySnap.get("memberIds") || [];

    // Collect FCM tokens for everyone except the author.
    const tokens = [];
    await Promise.all(
      memberIds
        .filter((uid) => uid !== post.authorId)
        .map(async (uid) => {
          const userSnap = await db.collection("users").doc(uid).get();
          const userTokens = userSnap.get("fcmTokens") || [];
          tokens.push(...userTokens);
        })
    );

    const unique = [...new Set(tokens)].filter(Boolean);
    if (unique.length === 0) return;

    const kind = TYPE_LABELS[post.type] || "something new";
    const message = {
      notification: {
        title: `${post.authorName} shared ${kind}`,
        body: post.title || "Open FamilyNest to see it",
      },
      data: { postId, familyId },
      tokens: unique,
    };

    const response = await getMessaging().sendEachForMulticast(message);

    // Prune tokens that are no longer valid.
    const stale = [];
    response.responses.forEach((res, i) => {
      if (!res.success) {
        const code = res.error?.code || "";
        if (
          code.includes("registration-token-not-registered") ||
          code.includes("invalid-argument")
        ) {
          stale.push(unique[i]);
        }
      }
    });
    if (stale.length) {
      await Promise.all(
        memberIds.map((uid) =>
          db
            .collection("users")
            .doc(uid)
            .update({
              fcmTokens:
                require("firebase-admin/firestore").FieldValue.arrayRemove(
                  ...stale
                ),
            })
            .catch(() => {})
        )
      );
    }
  }
);

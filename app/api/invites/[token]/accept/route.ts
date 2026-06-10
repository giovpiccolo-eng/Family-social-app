import { NextRequest, NextResponse } from "next/server";
import { FieldValue } from "firebase-admin/firestore";
import { adminAuth, adminDb } from "@/lib/firebase/admin";

// POST /api/invites/:token/accept
// Headers: Authorization: Bearer <Firebase ID token>
// Body: { displayName }
//
// Validates the invite, sets custom claims (schoolId + role) on the user,
// writes users/{uid}, and marks the invite accepted.
export async function POST(
  req: NextRequest,
  { params }: { params: { token: string } },
) {
  const authz = req.headers.get("authorization") ?? "";
  const idToken = authz.startsWith("Bearer ") ? authz.slice(7) : null;
  if (!idToken) {
    return NextResponse.json({ error: "Missing bearer token" }, { status: 401 });
  }

  let decoded;
  try {
    decoded = await adminAuth().verifyIdToken(idToken);
  } catch {
    return NextResponse.json({ error: "Invalid token" }, { status: 401 });
  }

  const { displayName } = (await req.json().catch(() => ({}))) as {
    displayName?: string;
  };
  if (!displayName) {
    return NextResponse.json({ error: "displayName required" }, { status: 400 });
  }

  const inviteRef = adminDb().collection("invites").doc(params.token);
  const inviteSnap = await inviteRef.get();
  if (!inviteSnap.exists) {
    return NextResponse.json({ error: "Invite not found" }, { status: 404 });
  }
  const invite = inviteSnap.data()!;
  if (invite.acceptedAt) {
    return NextResponse.json({ error: "Invite already used" }, { status: 410 });
  }
  if (invite.expiresAt?.toMillis?.() && invite.expiresAt.toMillis() < Date.now()) {
    return NextResponse.json({ error: "Invite expired" }, { status: 410 });
  }
  if (
    invite.email &&
    decoded.email &&
    invite.email.toLowerCase() !== decoded.email.toLowerCase()
  ) {
    return NextResponse.json(
      { error: "Email does not match invite" },
      { status: 403 },
    );
  }

  const { schoolId, role } = invite as { schoolId: string; role: string };

  await adminAuth().setCustomUserClaims(decoded.uid, { schoolId, role });

  await adminDb()
    .collection("users")
    .doc(decoded.uid)
    .set(
      {
        uid: decoded.uid,
        email: decoded.email ?? invite.email,
        displayName,
        role,
        schoolId,
        createdAt: FieldValue.serverTimestamp(),
      },
      { merge: true },
    );

  await inviteRef.update({
    acceptedAt: FieldValue.serverTimestamp(),
    acceptedBy: decoded.uid,
  });

  return NextResponse.json({ ok: true, schoolId, role });
}

import "server-only";
import type { NextRequest } from "next/server";
import { adminAuth } from "./admin";
import type { Role } from "@/types";

export interface AuthenticatedRequest {
  uid: string;
  email?: string;
  schoolId: string;
  role: Role;
}

export class AuthError extends Error {
  constructor(public status: number, message: string) {
    super(message);
  }
}

// Verify the Authorization: Bearer <ID token> header and return the resolved
// schoolId and role from custom claims. Throws AuthError with HTTP status
// so route handlers can convert to a NextResponse.
export async function verifyRequest(
  req: NextRequest,
): Promise<AuthenticatedRequest> {
  const authz = req.headers.get("authorization") ?? "";
  const idToken = authz.startsWith("Bearer ") ? authz.slice(7) : null;
  if (!idToken) throw new AuthError(401, "Missing bearer token");

  let decoded;
  try {
    decoded = await adminAuth().verifyIdToken(idToken);
  } catch {
    throw new AuthError(401, "Invalid token");
  }

  const schoolId = decoded.schoolId as string | undefined;
  const role = decoded.role as Role | undefined;
  if (!schoolId || !role) {
    throw new AuthError(403, "Missing schoolId/role claims");
  }
  return { uid: decoded.uid, email: decoded.email, schoolId, role };
}

"use client";

import {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react";
import {
  onIdTokenChanged,
  signOut as fbSignOut,
  type User as FirebaseUser,
} from "firebase/auth";
import { getFirebase } from "./client";
import type { AuthClaims, Role } from "@/types";

interface AuthState {
  user: FirebaseUser | null;
  claims: AuthClaims | null;
  loading: boolean;
  signOut: () => Promise<void>;
}

const AuthContext = createContext<AuthState>({
  user: null,
  claims: null,
  loading: true,
  signOut: async () => {},
});

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<FirebaseUser | null>(null);
  const [claims, setClaims] = useState<AuthClaims | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const { auth } = getFirebase();
    const unsub = onIdTokenChanged(auth, async (u) => {
      setUser(u);
      if (u) {
        const token = await u.getIdTokenResult();
        const schoolId = token.claims.schoolId as string | undefined;
        const role = token.claims.role as Role | undefined;
        setClaims(schoolId && role ? { schoolId, role } : null);
      } else {
        setClaims(null);
      }
      setLoading(false);
    });
    return () => unsub();
  }, []);

  async function signOut() {
    const { auth } = getFirebase();
    await fbSignOut(auth);
  }

  return (
    <AuthContext.Provider value={{ user, claims, loading, signOut }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}

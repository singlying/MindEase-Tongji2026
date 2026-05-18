import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from "react";
import { authApi, type UserInfo } from "@/api";

interface SessionContextValue {
  token: string;
  user: UserInfo | null;
  isLoggedIn: boolean;
  setToken: (token: string) => void;
  setUser: (user: UserInfo) => void;
  fetchUser: () => Promise<UserInfo>;
  logout: () => void;
}

const SessionContext = createContext<SessionContextValue | null>(null);

export function SessionProvider({ children }: { children: ReactNode }) {
  const [tokenValue, setTokenValue] = useState(() => localStorage.getItem("token") || "");
  const [user, setUser] = useState<UserInfo | null>(null);

  const setToken = useCallback((token: string) => {
    setTokenValue(token);
    localStorage.setItem("token", token);
  }, []);

  const fetchUser = useCallback(async () => {
    const response = await authApi.profile();
    setUser(response.data);
    return response.data;
  }, []);

  const logout = useCallback(() => {
    setTokenValue("");
    setUser(null);
    localStorage.removeItem("token");
  }, []);

  const value = useMemo(
    () => ({ token: tokenValue, user, isLoggedIn: Boolean(tokenValue), setToken, setUser, fetchUser, logout }),
    [fetchUser, logout, setToken, tokenValue, user]
  );

  return <SessionContext.Provider value={value}>{children}</SessionContext.Provider>;
}

export function useSession() {
  const value = useContext(SessionContext);
  if (!value) throw new Error("useSession must be used inside SessionProvider");
  return value;
}

export function homeForUser(role?: string, status?: number) {
  const normalized = role?.toUpperCase();
  if (normalized === "COUNSELOR") return status === 2 ? "/counselor/audit-pending" : "/counselor/dashboard";
  if (normalized === "ADMIN") return "/admin/dashboard";
  return "/home";
}

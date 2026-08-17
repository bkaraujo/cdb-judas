/** Auth + bootstrap orchestration. */
import type { AuthStore } from '../_2_infrastructure/secondary/auth-store.ts';
import type { SseClient } from '../_2_infrastructure/secondary/sse-client.ts';

export interface LoginResult {
  token: string;
  userId: string;
}

export interface SessionServiceDeps {
  authStore: AuthStore;
  loginFn: (user: string, password: string) => Promise<LoginResult>;
  hydrate: () => Promise<unknown>;
  sse?: SseClient;
}

export interface SessionService {
  isAuthenticated(): boolean;
  login(user: string, password: string): Promise<unknown>;
  logout(): void;
  bootstrap(): Promise<unknown>;
  startSSE(): void;
  stopSSE(): void;
  onUnauthorized(cb: () => void): void;
  handleUnauthorized(): void;
}

export function createSessionService(deps: SessionServiceDeps): SessionService {
  const { authStore, loginFn, hydrate, sse } = deps;
  let onUnauthorizedCb: () => void = () => {};

  function login(user: string, password: string): Promise<unknown> {
    return loginFn(user, password)
      .then((result) => {
        // A missing X-User-Id must not produce a session that "works" (valid token) but silently
        // 404s every /api/{uuid}/... call via withUser's empty-prefix fallback (http-client.ts) —
        // fail loudly here instead of leaving a half-authenticated session to surface as a random
        // 404 minutes later on an unrelated feature.
        if (!result.userId) throw new Error('Login sem X-User-Id na resposta');
        authStore.set(result.token);
        authStore.setUser(user);
        authStore.setUserId(result.userId);
        return hydrate();
      })
      .catch((e) => {
        authStore.clear();
        throw e;
      });
  }

  function logout(): void {
    if (sse) sse.disconnect();
    authStore.clear();
    window.location.reload();
  }

  function handleUnauthorized(): void {
    if (sse) sse.disconnect();
    try {
      onUnauthorizedCb();
    } catch {
      /* noop */
    }
  }

  return {
    isAuthenticated: () => !!authStore.get(),
    login,
    logout,
    bootstrap: () => hydrate(),
    startSSE: () => {
      if (sse) sse.connect();
    },
    stopSSE: () => {
      if (sse) sse.disconnect();
    },
    onUnauthorized: (cb) => {
      onUnauthorizedCb = cb || (() => {});
    },
    handleUnauthorized,
  };
}

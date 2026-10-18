export type Role = 'USER' | 'ADMIN';

export type AuthUser = {
  id: number;
  name: string;
  email: string;
  role: Role;
  companyId: number;
  companyLocationId: number;
};

export type SessionResponse = {
  user: Pick<AuthUser, 'name' | 'role'>;
};

// Server-only: never sent to the browser.
export type TokenPair = {
  accessToken: string;
  accessTokenExpiresAt: string;
  refreshToken: string;
  refreshTokenExpiresAt: string;
  user: AuthUser;
};

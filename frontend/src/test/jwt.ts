import type { Role } from '@/lib/auth/token-pair';

// Unsigned JWT with the claims the frontend decodes; the signature is never checked here
export function fakeJwt(role: Role, expiresInMs: number): string {
  const encode = (value: object) =>
    Buffer.from(JSON.stringify(value)).toString('base64url');
  const exp = Math.floor((Date.now() + expiresInMs) / 1000);
  return `${encode({ alg: 'HS256' })}.${encode({ sub: '1', role, exp })}.signature`;
}

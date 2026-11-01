import type { Role } from '@/lib/auth/token-pair';
import { serverApi } from './server';

type Reference = { id: number; name: string };

export type Me = {
  id: number;
  name: string;
  email: string;
  nif: string | null;
  contact: string | null;
  role: Role;
  company: Reference;
  companyLocation: Reference;
};

export function getMe(): Promise<Me> {
  return serverApi<Me>('/me');
}

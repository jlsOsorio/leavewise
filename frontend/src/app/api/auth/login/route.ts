import type { NextRequest } from 'next/server';

import { exchangeForSession } from '@/lib/auth/backend';
import { loginSchema } from '@/lib/schemas/auth';

export function POST(request: NextRequest) {
  return exchangeForSession('/login', request, loginSchema);
}

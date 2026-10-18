import type { NextRequest } from 'next/server';

import { exchangeForSession } from '@/lib/auth/backend';

export function POST(request: NextRequest) {
  return exchangeForSession('/registration', request);
}

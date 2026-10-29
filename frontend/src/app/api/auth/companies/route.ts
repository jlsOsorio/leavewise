import type { NextRequest } from 'next/server';

import { getPublicAuth } from '@/lib/auth/backend';

const PICKER_PAGE_SIZE = 20;

export async function GET(request: NextRequest) {
  return getPublicAuth('/companies', {
    q: request.nextUrl.searchParams.get('q') ?? '',
    page: '0',
    size: String(PICKER_PAGE_SIZE),
    sort: 'name,asc',
  });
}

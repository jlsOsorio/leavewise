import type { NextRequest } from 'next/server';

import { getWithSession } from '@/lib/api/proxy-get';

const PICKER_PAGE_SIZE = 20;

export async function GET(request: NextRequest) {
  return getWithSession('/municipalities', {
    q: request.nextUrl.searchParams.get('q') ?? '',
    page: '0',
    size: String(PICKER_PAGE_SIZE),
    sort: 'name,asc',
  });
}

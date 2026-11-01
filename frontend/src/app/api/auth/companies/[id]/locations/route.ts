import type { NextRequest } from 'next/server';

import { getPublicAuth } from '@/lib/auth/backend';

export async function GET(
  _request: NextRequest,
  { params }: RouteContext<'/api/auth/companies/[id]/locations'>,
) {
  const { id } = await params;
  return getPublicAuth(`/companies/${encodeURIComponent(id)}/locations`, {
    page: '0',
    size: '100',
    sort: 'name,asc',
  });
}

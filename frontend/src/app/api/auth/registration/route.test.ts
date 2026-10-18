// @vitest-environment node
import { http, HttpResponse } from 'msw';
import { NextRequest } from 'next/server';
import { describe, expect, test } from 'vitest';

import { server } from '@/test/msw/server';
import { POST } from './route';

const registrationRequest = () =>
  new NextRequest('http://localhost:3000/api/auth/registration', {
    method: 'POST',
    body: JSON.stringify({
      name: 'Ana Silva',
      email: 'ana@example.pt',
      password: 'secret123',
      companyId: 1,
      companyLocationId: 1,
    }),
  });

describe('POST /api/auth/registration', () => {
  test('returns 201 and sets both cookies', async () => {
    const response = await POST(registrationRequest());

    expect(response.status).toBe(201);
    expect(response.cookies.get('lw_at')?.value).toBe('access-jwt');
    expect(response.cookies.get('lw_rt')?.value).toBe('refresh-token');
  });

  test('passes a backend 409 EMAIL_TAKEN through without setting cookies', async () => {
    server.use(
      http.post(`${process.env.BACKEND_URL}/api/auth/registration`, () =>
        HttpResponse.json(
          { status: 409, code: 'EMAIL_TAKEN' },
          {
            status: 409,
            headers: { 'Content-Type': 'application/problem+json' },
          },
        ),
      ),
    );

    const response = await POST(registrationRequest());

    expect(response.status).toBe(409);
    expect(await response.json()).toMatchObject({ code: 'EMAIL_TAKEN' });
    expect(response.cookies.getAll()).toHaveLength(0);
  });
});

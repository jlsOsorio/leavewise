import { render, screen } from '@testing-library/react';
import { Users } from 'lucide-react';
import { http, HttpResponse } from 'msw';
import { describe, expect, test, vi } from 'vitest';

import { SidebarProvider } from '@/components/ui/sidebar';
import { TooltipProvider } from '@/components/ui/tooltip';
import { server } from '@/test/msw/server';
import { AdminNav } from './AdminNav';

vi.mock('next/headers', () => ({
  cookies: async () => ({
    get: (name: string) => ({ name, value: 'access-jwt' }),
  }),
}));

vi.mock('next/navigation', () => ({ usePathname: () => '/' }));

vi.mock('./nav', () => ({
  NAV: {
    main: [],
    admin: [{ href: '/admin/users', label: 'Users', icon: Users }],
  },
}));

function withRole(role: 'USER' | 'ADMIN') {
  server.use(
    http.get(`${process.env.BACKEND_URL}/api/me`, () =>
      HttpResponse.json({
        name: 'Ana Ferreira',
        role,
        company: { id: 1, name: 'Acme Lda' },
      }),
    ),
  );
}

async function renderNav() {
  const nav = await AdminNav();
  render(
    <TooltipProvider>
      <SidebarProvider>{nav}</SidebarProvider>
    </TooltipProvider>,
  );
}

describe('AdminNav', () => {
  test('shows the administration links to an ADMIN', async () => {
    withRole('ADMIN');

    await renderNav();

    expect(screen.getByRole('link', { name: 'Users' })).toBeInTheDocument();
  });

  test('hides the administration links from a USER', async () => {
    withRole('USER');

    await renderNav();

    expect(screen.queryByRole('link')).not.toBeInTheDocument();
  });
});

import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { delay, http, HttpResponse } from 'msw';
import { describe, expect, test, vi } from 'vitest';

import { page } from '@/test/msw/handlers';
import { server } from '@/test/msw/server';
import { CompanyCombobox } from './CompanyCombobox';

const COMPANIES_PATH = '/api/auth/companies';

function companiesByQuery(results: Record<string, string[]>, slow = '') {
  const queries: string[] = [];
  server.use(
    http.get(COMPANIES_PATH, async ({ request }) => {
      const q = new URL(request.url).searchParams.get('q') ?? '';
      queries.push(q);
      if (q === slow) {
        await delay(400);
      }
      const names = results[q] ?? [];
      return HttpResponse.json(
        page(names.map((name, index) => ({ id: index + 1, name }))),
      );
    }),
  );
  return queries;
}

function renderCombobox(onChange = vi.fn()) {
  render(
    <>
      <label htmlFor="company">Company</label>
      <CompanyCombobox id="company" onChange={onChange} invalid={false} />
    </>,
  );
  return { user: userEvent.setup(), onChange };
}

const openCombobox = (user: ReturnType<typeof userEvent.setup>) =>
  user.click(screen.getByRole('combobox', { name: 'Company' }));

describe('CompanyCombobox', () => {
  test('lists companies on open and reports the chosen one', async () => {
    companiesByQuery({ '': ['Acme Lda', 'Beta SA'] });
    const { user, onChange } = renderCombobox();

    await openCombobox(user);
    await user.click(await screen.findByRole('option', { name: 'Beta SA' }));

    expect(onChange).toHaveBeenCalledWith({ id: 2, name: 'Beta SA' });
    expect(screen.getByRole('combobox', { name: 'Company' })).toHaveTextContent(
      'Beta SA',
    );
  });

  test('searches once typing pauses, not on every keystroke', async () => {
    const queries = companiesByQuery({ '': ['Acme Lda'], acme: ['Acme Lda'] });
    const { user } = renderCombobox();

    await openCombobox(user);
    await screen.findByRole('option', { name: 'Acme Lda' });
    await user.type(screen.getByPlaceholderText('Type a company name'), 'acme');
    await vi.waitFor(() => expect(queries).toHaveLength(2));
    await delay(400);

    expect(queries).toEqual(['', 'acme']);
  });

  test('shows the latest search even when an older one answers later', async () => {
    companiesByQuery({ '': [], ol: ['Old Co'], old: ['New Co'] }, 'ol');
    const { user } = renderCombobox();
    await openCombobox(user);
    const input = screen.getByPlaceholderText('Type a company name');

    await user.type(input, 'ol');
    await delay(350); // the "ol" search is now in flight
    await user.type(input, 'd');

    expect(await screen.findByRole('option', { name: 'New Co' })).toBeVisible();
    await delay(400);
    expect(
      screen.queryByRole('option', { name: 'Old Co' }),
    ).not.toBeInTheDocument();
  });

  test('says so when no company matches', async () => {
    companiesByQuery({ '': [] });
    const { user } = renderCombobox();

    await openCombobox(user);

    expect(
      await screen.findByText('No company matches that name.'),
    ).toBeVisible();
  });

  test('says so when the search fails', async () => {
    server.use(
      http.get(COMPANIES_PATH, () => new HttpResponse(null, { status: 500 })),
    );
    const { user } = renderCombobox();

    await openCombobox(user);

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Companies could not be loaded.',
    );
  });
});

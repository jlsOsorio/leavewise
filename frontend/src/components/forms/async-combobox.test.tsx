import { render, screen } from '@testing-library/react';
import userEvent, { type UserEvent } from '@testing-library/user-event';
import { delay, http, HttpResponse } from 'msw';
import { describe, expect, test, vi } from 'vitest';

import { page } from '@/test/msw/handlers';
import { server } from '@/test/msw/server';
import { AsyncCombobox } from './async-combobox';

const SEARCH_PATH = '/api/test/items';

type Item = { id: number; name: string };

function itemsByQuery(results: Record<string, string[]>, slow = '') {
  const queries: string[] = [];
  server.use(
    http.get(SEARCH_PATH, async ({ request }) => {
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

function itemName(item: Item): string {
  return item.name;
}

function renderCombobox({
  onChange = vi.fn(),
  defaultValue,
}: { onChange?: (item: Item) => void; defaultValue?: Item } = {}) {
  render(
    <>
      <label htmlFor="item">Item</label>
      <AsyncCombobox
        id="item"
        searchPath={SEARCH_PATH}
        optionLabel={itemName}
        texts={{
          placeholder: 'Pick an item',
          searchPlaceholder: 'Type an item name',
          searching: 'Searching items…',
          empty: 'No item matches that name.',
          failed: 'Items could not be loaded.',
        }}
        defaultValue={defaultValue}
        onChange={onChange}
        invalid={false}
      />
    </>,
  );
  return { user: userEvent.setup(), onChange };
}

const openCombobox = (user: UserEvent) =>
  user.click(screen.getByRole('combobox', { name: 'Item' }));

describe('AsyncCombobox', () => {
  test('lists options on open and reports the chosen one', async () => {
    itemsByQuery({ '': ['Acme Lda', 'Beta SA'] });
    const { user, onChange } = renderCombobox();

    await openCombobox(user);
    await user.click(await screen.findByRole('option', { name: 'Beta SA' }));

    expect(onChange).toHaveBeenCalledWith({ id: 2, name: 'Beta SA' });
    expect(screen.getByRole('combobox', { name: 'Item' })).toHaveTextContent(
      'Beta SA',
    );
  });

  test('shows the default value before anything is chosen', () => {
    renderCombobox({ defaultValue: { id: 7, name: 'Braga' } });

    expect(screen.getByRole('combobox', { name: 'Item' })).toHaveTextContent(
      'Braga',
    );
  });

  test('searches once typing pauses, not on every keystroke', async () => {
    const queries = itemsByQuery({ '': ['Acme Lda'], acme: ['Acme Lda'] });
    const { user } = renderCombobox();

    await openCombobox(user);
    await screen.findByRole('option', { name: 'Acme Lda' });
    await user.type(screen.getByPlaceholderText('Type an item name'), 'acme');
    await vi.waitFor(() => expect(queries).toHaveLength(2));
    await delay(400);

    expect(queries).toEqual(['', 'acme']);
  });

  test('shows the latest search even when an older one answers later', async () => {
    itemsByQuery({ '': [], ol: ['Old Co'], old: ['New Co'] }, 'ol');
    const { user } = renderCombobox();
    await openCombobox(user);
    const input = screen.getByPlaceholderText('Type an item name');

    await user.type(input, 'ol');
    await delay(350); // the "ol" search is now in flight
    await user.type(input, 'd');

    expect(await screen.findByRole('option', { name: 'New Co' })).toBeVisible();
    await delay(400);
    expect(
      screen.queryByRole('option', { name: 'Old Co' }),
    ).not.toBeInTheDocument();
  });

  test('tells how many options match when only the first page is listed', async () => {
    server.use(
      http.get(SEARCH_PATH, () =>
        HttpResponse.json(page([{ id: 1, name: 'Acme Lda' }], 500)),
      ),
    );
    const { user } = renderCombobox();

    await openCombobox(user);

    expect(
      await screen.findByText(/Showing 1 of 500\. Keep typing/),
    ).toBeVisible();
  });

  test('says so when no option matches', async () => {
    itemsByQuery({ '': [] });
    const { user } = renderCombobox();

    await openCombobox(user);

    expect(await screen.findByText('No item matches that name.')).toBeVisible();
  });

  test('says so when the search fails', async () => {
    server.use(
      http.get(SEARCH_PATH, () => new HttpResponse(null, { status: 500 })),
    );
    const { user } = renderCombobox();

    await openCombobox(user);

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Items could not be loaded.',
    );
  });
});

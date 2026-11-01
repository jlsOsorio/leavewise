import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, test, vi } from 'vitest';

import { page } from '@/test/msw/handlers';
import { server } from '@/test/msw/server';
import { loadLocations, LocationSelect } from './LocationSelect';

const LOCATIONS_PATH = '/api/auth/companies/:id/locations';

async function renderSelect(
  locations: ReturnType<typeof loadLocations> | null,
  onChange = vi.fn(),
) {
  // React 19 retries a suspended render only inside an awaited act()
  // eslint-disable-next-line testing-library/no-unnecessary-act
  await act(async () => {
    render(
      <>
        <label htmlFor="location">Location</label>
        <LocationSelect
          id="location"
          locations={locations}
          value={undefined}
          onChange={onChange}
          invalid={false}
        />
      </>,
    );
  });
}

describe('LocationSelect', () => {
  test('stays disabled until a company is chosen', async () => {
    await renderSelect(null);

    expect(screen.getByRole('combobox', { name: 'Location' })).toBeDisabled();
  });

  test('reports the id of the chosen location', async () => {
    server.use(
      http.get(LOCATIONS_PATH, () =>
        HttpResponse.json(
          page([
            { id: 1, name: 'Lisboa' },
            { id: 2, name: 'Porto' },
          ]),
        ),
      ),
    );
    const onChange = vi.fn();
    await renderSelect(loadLocations(1), onChange);
    const user = userEvent.setup();

    await user.click(screen.getByRole('combobox', { name: 'Location' }));
    await user.click(await screen.findByRole('option', { name: 'Porto' }));

    expect(onChange).toHaveBeenCalledWith(2);
  });

  test('says so when the locations cannot be loaded', async () => {
    server.use(
      http.get(LOCATIONS_PATH, () => new HttpResponse(null, { status: 500 })),
    );

    await renderSelect(loadLocations(1));

    expect(screen.getByText('Locations could not be loaded')).toBeVisible();
  });
});

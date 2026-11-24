import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, test, vi } from 'vitest';

import { Button } from '@/components/ui/button';
import type { LocationDetails } from '@/lib/api/locations';
import { LocationFormDialog } from './location-form-dialog';

const actions = vi.hoisted(() => ({
  createLocation: vi.fn(),
  updateLocation: vi.fn(),
}));
vi.mock('../actions', () => actions);

const sede: LocationDetails = {
  id: 11,
  companyId: 3,
  name: 'Sede',
  address: null,
  municipality: {
    id: 5,
    name: 'Braga',
    district: 'Braga',
    country: 'Portugal',
  },
};

beforeEach(() => {
  actions.createLocation.mockReset();
  actions.updateLocation.mockReset();
});

async function openDialog(location?: LocationDetails) {
  const user = userEvent.setup();
  render(
    <LocationFormDialog companyId={3} location={location}>
      <Button>Open</Button>
    </LocationFormDialog>,
  );
  await user.click(screen.getByRole('button', { name: 'Open' }));
  return user;
}

describe('LocationFormDialog', () => {
  test('creates a location in the chosen municipality and closes', async () => {
    actions.createLocation.mockResolvedValue({ ok: true });
    const user = await openDialog();

    await user.type(screen.getByLabelText(/Name/), 'Sede');
    await user.type(screen.getByLabelText(/Address/), 'Rua do Souto, 1');
    await user.click(screen.getByRole('combobox', { name: /Municipality/ }));
    await user.click(
      await screen.findByRole('option', { name: 'Guimarães (Braga)' }),
    );
    await user.click(screen.getByRole('button', { name: 'Save' }));

    expect(actions.createLocation).toHaveBeenCalledWith(3, {
      name: 'Sede',
      address: 'Rua do Souto, 1',
      municipalityId: 6,
    });
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  test('keeps the current municipality when editing other fields', async () => {
    actions.updateLocation.mockResolvedValue({ ok: true });
    const user = await openDialog(sede);

    expect(
      screen.getByRole('combobox', { name: /Municipality/ }),
    ).toHaveTextContent('Braga (Braga)');
    await user.type(screen.getByLabelText(/Address/), 'Rua do Souto, 1');
    await user.click(screen.getByRole('button', { name: 'Save' }));

    expect(actions.updateLocation).toHaveBeenCalledWith(3, 11, {
      name: 'Sede',
      address: 'Rua do Souto, 1',
      municipalityId: 5,
    });
  });

  test('asks for a municipality without calling the action', async () => {
    const user = await openDialog();

    await user.type(screen.getByLabelText(/Name/), 'Sede');
    await user.click(screen.getByRole('button', { name: 'Save' }));

    expect(await screen.findByText('Choose the municipality.')).toBeVisible();
    expect(actions.createLocation).not.toHaveBeenCalled();
  });

  test('stays open and shows a duplicate as a form error', async () => {
    actions.updateLocation.mockResolvedValue({
      ok: false,
      error: { code: 'DUPLICATE', message: 'This item already exists.' },
    });
    const user = await openDialog(sede);

    await user.click(screen.getByRole('button', { name: 'Save' }));

    expect(await screen.findByText('This item already exists.')).toBeVisible();
    expect(screen.getByRole('dialog')).toBeVisible();
  });
});

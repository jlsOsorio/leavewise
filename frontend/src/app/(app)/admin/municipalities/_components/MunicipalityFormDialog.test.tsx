import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, test, vi } from 'vitest';

import { Button } from '@/components/ui/button';
import { MunicipalityFormDialog } from './MunicipalityFormDialog';

const actions = vi.hoisted(() => ({
  createMunicipality: vi.fn(),
  updateMunicipality: vi.fn(),
}));
vi.mock('../actions', () => actions);

const braga = { id: 5, name: 'Braga', district: 'Braga', country: 'Portugal' };

beforeEach(() => {
  actions.createMunicipality.mockReset();
  actions.updateMunicipality.mockReset();
});

async function openDialog(municipality?: typeof braga) {
  const user = userEvent.setup();
  render(
    <MunicipalityFormDialog municipality={municipality}>
      <Button>Open</Button>
    </MunicipalityFormDialog>,
  );
  await user.click(screen.getByRole('button', { name: 'Open' }));
  return user;
}

describe('MunicipalityFormDialog', () => {
  test('creates a municipality with Portugal as the default country and closes', async () => {
    actions.createMunicipality.mockResolvedValue({ ok: true });
    const user = await openDialog();

    await user.type(screen.getByLabelText(/Name/), 'Braga');
    await user.type(screen.getByLabelText(/District/), 'Braga');
    await user.click(screen.getByRole('button', { name: 'Save' }));

    expect(actions.createMunicipality).toHaveBeenCalledWith({
      name: 'Braga',
      district: 'Braga',
      country: 'Portugal',
    });
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  test('saves changes to an existing municipality', async () => {
    actions.updateMunicipality.mockResolvedValue({ ok: true });
    const user = await openDialog(braga);

    await user.clear(screen.getByLabelText(/District/));
    await user.type(screen.getByLabelText(/District/), 'Minho');
    await user.click(screen.getByRole('button', { name: 'Save' }));

    expect(actions.updateMunicipality).toHaveBeenCalledWith(5, {
      name: 'Braga',
      district: 'Minho',
      country: 'Portugal',
    });
  });

  test('shows a missing name without calling the action', async () => {
    const user = await openDialog();

    await user.type(screen.getByLabelText(/District/), 'Braga');
    await user.click(screen.getByRole('button', { name: 'Save' }));

    expect(await screen.findByText('Enter the name.')).toBeVisible();
    expect(actions.createMunicipality).not.toHaveBeenCalled();
  });

  test('stays open and shows a duplicate as a form error', async () => {
    actions.createMunicipality.mockResolvedValue({
      ok: false,
      error: { code: 'DUPLICATE', message: 'This item already exists.' },
    });
    const user = await openDialog();

    await user.type(screen.getByLabelText(/Name/), 'Braga');
    await user.type(screen.getByLabelText(/District/), 'Braga');
    await user.click(screen.getByRole('button', { name: 'Save' }));

    expect(await screen.findByText('This item already exists.')).toBeVisible();
    expect(screen.getByRole('dialog')).toBeVisible();
  });
});

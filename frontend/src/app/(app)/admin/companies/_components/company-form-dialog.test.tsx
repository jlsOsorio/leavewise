import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, test, vi } from 'vitest';

import { Button } from '@/components/ui/button';
import { CompanyFormDialog } from './company-form-dialog';

const actions = vi.hoisted(() => ({
  createCompany: vi.fn(),
  updateCompany: vi.fn(),
}));
vi.mock('../actions', () => actions);

const acme = { id: 3, name: 'Acme Lda' };

beforeEach(() => {
  actions.createCompany.mockReset();
  actions.updateCompany.mockReset();
});

async function openDialog(company?: typeof acme) {
  const user = userEvent.setup();
  render(
    <CompanyFormDialog company={company}>
      <Button>Open</Button>
    </CompanyFormDialog>,
  );
  await user.click(screen.getByRole('button', { name: 'Open' }));
  return user;
}

describe('CompanyFormDialog', () => {
  test('creates a company and closes', async () => {
    actions.createCompany.mockResolvedValue({ ok: true });
    const user = await openDialog();

    await user.type(screen.getByLabelText(/Name/), 'Acme Lda');
    await user.click(screen.getByRole('button', { name: 'Save' }));

    expect(actions.createCompany).toHaveBeenCalledWith({ name: 'Acme Lda' });
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  test('renames an existing company', async () => {
    actions.updateCompany.mockResolvedValue({ ok: true });
    const user = await openDialog(acme);

    await user.clear(screen.getByLabelText(/Name/));
    await user.type(screen.getByLabelText(/Name/), 'Acme SA');
    await user.click(screen.getByRole('button', { name: 'Save' }));

    expect(actions.updateCompany).toHaveBeenCalledWith(3, { name: 'Acme SA' });
  });

  test('shows a missing name without calling the action', async () => {
    const user = await openDialog();

    await user.click(screen.getByRole('button', { name: 'Save' }));

    expect(await screen.findByText('Enter the name.')).toBeVisible();
    expect(actions.createCompany).not.toHaveBeenCalled();
  });

  test('stays open and shows a duplicate as a form error', async () => {
    actions.createCompany.mockResolvedValue({
      ok: false,
      error: { code: 'DUPLICATE', message: 'This item already exists.' },
    });
    const user = await openDialog();

    await user.type(screen.getByLabelText(/Name/), 'Acme Lda');
    await user.click(screen.getByRole('button', { name: 'Save' }));

    expect(await screen.findByText('This item already exists.')).toBeVisible();
    expect(screen.getByRole('dialog')).toBeVisible();
  });
});

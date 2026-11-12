import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { toast } from 'sonner';
import { afterEach, describe, expect, test, vi } from 'vitest';

import { Button } from '@/components/ui/button';
import { Toaster } from '@/components/ui/sonner';
import type { ActionResult } from '@/lib/api/action-result';
import { ConfirmDialog } from './ConfirmDialog';

afterEach(() => {
  toast.dismiss();
});

function renderDialog(onConfirm: () => Promise<ActionResult>) {
  render(
    <>
      <ConfirmDialog
        title="Delete Braga?"
        description="This can't be undone."
        confirmLabel="Delete"
        successMessage="Municipality deleted."
        onConfirm={onConfirm}
      >
        <Button>Open</Button>
      </ConfirmDialog>
      <Toaster />
    </>,
  );
}

describe('ConfirmDialog', () => {
  test('closes and confirms with a toast when the action succeeds', async () => {
    const user = userEvent.setup();
    renderDialog(async () => ({ ok: true }));

    await user.click(screen.getByRole('button', { name: 'Open' }));
    await user.click(screen.getByRole('button', { name: 'Delete' }));

    expect(await screen.findByText('Municipality deleted.')).toBeVisible();
    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
  });

  test('stays open with the error when the action fails', async () => {
    const user = userEvent.setup();
    renderDialog(async () => ({
      ok: false,
      error: {
        code: 'RESOURCE_IN_USE',
        message: "This item is in use and can't be deleted.",
      },
    }));

    await user.click(screen.getByRole('button', { name: 'Open' }));
    await user.click(screen.getByRole('button', { name: 'Delete' }));

    expect(
      await screen.findByText("This item is in use and can't be deleted."),
    ).toBeVisible();
    expect(screen.getByRole('alertdialog')).toBeVisible();
  });

  test('does not run the action when cancelled', async () => {
    const user = userEvent.setup();
    const onConfirm = vi.fn(async (): Promise<ActionResult> => ({ ok: true }));
    renderDialog(onConfirm);

    await user.click(screen.getByRole('button', { name: 'Open' }));
    await user.click(screen.getByRole('button', { name: 'Cancel' }));

    expect(onConfirm).not.toHaveBeenCalled();
  });
});

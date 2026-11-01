import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { expect, test, vi } from 'vitest';

import Error from './error';

test('retries loading the page when asked', async () => {
  const retry = vi.fn();
  render(
    <Error
      error={new globalThis.Error('boom')}
      retry={retry}
      reset={vi.fn()}
    />,
  );
  const user = userEvent.setup();

  await user.click(screen.getByRole('button', { name: 'Try again' }));

  expect(retry).toHaveBeenCalledOnce();
});

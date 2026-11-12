import { revalidatePath } from 'next/cache';
import * as z from 'zod';

import { type ActionResult, apiFailure } from './action-result';
import { ApiError } from './api-error';

export const resourceIdSchema = z.number().int().positive();

export async function mutate(
  request: () => Promise<unknown>,
  revalidate: string,
): Promise<ActionResult> {
  try {
    await request();
  } catch (error) {
    if (error instanceof ApiError) {
      return apiFailure(error);
    }
    throw error;
  }
  revalidatePath(revalidate);
  return { ok: true };
}

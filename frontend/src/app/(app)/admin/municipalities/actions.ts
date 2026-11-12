'use server';

import { type ActionResult, validationFailure } from '@/lib/api/action-result';
import { mutate, resourceIdSchema } from '@/lib/api/mutate';
import { serverApi } from '@/lib/api/server';
import {
  type MunicipalityInput,
  municipalitySchema,
} from '@/lib/schemas/municipality';

const PAGE_PATH = '/admin/municipalities';

export async function createMunicipality(
  input: MunicipalityInput,
): Promise<ActionResult> {
  const parsed = municipalitySchema.safeParse(input);
  if (!parsed.success) {
    return validationFailure(parsed.error);
  }
  return mutate(
    () => serverApi('/municipalities', { method: 'POST', body: parsed.data }),
    PAGE_PATH,
  );
}

export async function updateMunicipality(
  id: number,
  input: MunicipalityInput,
): Promise<ActionResult> {
  const parsed = municipalitySchema.safeParse(input);
  if (!parsed.success) {
    return validationFailure(parsed.error);
  }
  const municipalityId = resourceIdSchema.parse(id);
  return mutate(
    () =>
      serverApi(`/municipalities/${municipalityId}`, {
        method: 'PUT',
        body: parsed.data,
      }),
    PAGE_PATH,
  );
}

export async function deleteMunicipality(id: number): Promise<ActionResult> {
  const municipalityId = resourceIdSchema.parse(id);
  return mutate(
    () =>
      serverApi<void>(`/municipalities/${municipalityId}`, {
        method: 'DELETE',
      }),
    PAGE_PATH,
  );
}

'use server';

import { type ActionResult, validationFailure } from '@/lib/api/action-result';
import { mutate, resourceIdSchema } from '@/lib/api/mutate';
import { serverApi } from '@/lib/api/server';
import { type LocationInput, locationSchema } from '@/lib/schemas/location';

function companyPath(companyId: number): string {
  return `/admin/companies/${companyId}`;
}

export async function createLocation(
  companyId: number,
  input: LocationInput,
): Promise<ActionResult> {
  const parsed = locationSchema.safeParse(input);
  if (!parsed.success) {
    return validationFailure(parsed.error);
  }
  const validCompanyId = resourceIdSchema.parse(companyId);
  return mutate(
    () =>
      serverApi(`/companies/${validCompanyId}/locations`, {
        method: 'POST',
        body: parsed.data,
      }),
    companyPath(validCompanyId),
  );
}

export async function updateLocation(
  companyId: number,
  id: number,
  input: LocationInput,
): Promise<ActionResult> {
  const parsed = locationSchema.safeParse(input);
  if (!parsed.success) {
    return validationFailure(parsed.error);
  }
  const validCompanyId = resourceIdSchema.parse(companyId);
  const locationId = resourceIdSchema.parse(id);
  return mutate(
    () =>
      serverApi(`/locations/${locationId}`, {
        method: 'PUT',
        body: parsed.data,
      }),
    companyPath(validCompanyId),
  );
}

export async function deleteLocation(
  companyId: number,
  id: number,
): Promise<ActionResult> {
  const validCompanyId = resourceIdSchema.parse(companyId);
  const locationId = resourceIdSchema.parse(id);
  return mutate(
    () => serverApi<void>(`/locations/${locationId}`, { method: 'DELETE' }),
    companyPath(validCompanyId),
  );
}

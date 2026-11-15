'use server';

import { type ActionResult, validationFailure } from '@/lib/api/action-result';
import { mutate, resourceIdSchema } from '@/lib/api/mutate';
import { serverApi } from '@/lib/api/server';
import { type CompanyInput, companySchema } from '@/lib/schemas/company';

const PAGE_PATH = '/admin/companies';

export async function createCompany(
  input: CompanyInput,
): Promise<ActionResult> {
  const parsed = companySchema.safeParse(input);
  if (!parsed.success) {
    return validationFailure(parsed.error);
  }
  return mutate(
    () => serverApi('/companies', { method: 'POST', body: parsed.data }),
    PAGE_PATH,
  );
}

export async function updateCompany(
  id: number,
  input: CompanyInput,
): Promise<ActionResult> {
  const parsed = companySchema.safeParse(input);
  if (!parsed.success) {
    return validationFailure(parsed.error);
  }
  const companyId = resourceIdSchema.parse(id);
  return mutate(
    () =>
      serverApi(`/companies/${companyId}`, {
        method: 'PUT',
        body: parsed.data,
      }),
    PAGE_PATH,
  );
}

export async function deleteCompany(id: number): Promise<ActionResult> {
  const companyId = resourceIdSchema.parse(id);
  return mutate(
    () => serverApi<void>(`/companies/${companyId}`, { method: 'DELETE' }),
    PAGE_PATH,
  );
}

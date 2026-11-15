import * as z from 'zod';

export const COMPANY_NAME_MAX_LENGTH = 200;

export const companySchema = z.object({
  name: z
    .string()
    .trim()
    .min(1, 'Enter the name.')
    .max(COMPANY_NAME_MAX_LENGTH, 'Name is too long.'),
});

export type CompanyInput = z.infer<typeof companySchema>;

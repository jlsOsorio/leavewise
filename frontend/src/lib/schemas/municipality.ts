import * as z from 'zod';

export const MUNICIPALITY_NAME_MAX_LENGTH = 150;
const DISTRICT_MAX_LENGTH = 150;
const COUNTRY_MAX_LENGTH = 100;

export const municipalitySchema = z.object({
  name: z
    .string()
    .trim()
    .min(1, 'Enter the name.')
    .max(MUNICIPALITY_NAME_MAX_LENGTH, 'Name is too long.'),
  district: z
    .string()
    .trim()
    .min(1, 'Enter the district.')
    .max(DISTRICT_MAX_LENGTH, 'District is too long.'),
  country: z.string().trim().max(COUNTRY_MAX_LENGTH, 'Country is too long.'),
});

export type MunicipalityInput = z.infer<typeof municipalitySchema>;

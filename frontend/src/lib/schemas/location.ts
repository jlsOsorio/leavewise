import * as z from 'zod';

const NAME_MAX_LENGTH = 150;
const ADDRESS_MAX_LENGTH = 500;

export const locationSchema = z.object({
  name: z
    .string()
    .trim()
    .min(1, 'Enter the name.')
    .max(NAME_MAX_LENGTH, 'Name is too long.'),
  address: z.string().trim().max(ADDRESS_MAX_LENGTH, 'Address is too long.'),
  municipalityId: z.number({ error: 'Choose the municipality.' }),
});

export type LocationInput = z.infer<typeof locationSchema>;

import * as z from 'zod';

import { isValidNif } from './nif';

const NAME_MAX_LENGTH = 200;
const EMAIL_MAX_LENGTH = 320;
const CONTACT_MAX_LENGTH = 50;
const PASSWORD_MIN_LENGTH = 8;
const PASSWORD_MAX_UTF8_BYTES = 72;

const emailSchema = z
  .string()
  .trim()
  .pipe(
    z
      .email('Enter a valid email address.')
      .max(EMAIL_MAX_LENGTH, 'Email is too long.'),
  );

export const passwordSchema = z
  .string()
  .min(PASSWORD_MIN_LENGTH, 'Use at least 8 characters.')
  .refine(
    (password) =>
      new TextEncoder().encode(password).length <= PASSWORD_MAX_UTF8_BYTES,
    'Password is too long.',
  );

function optionalText(schema: z.ZodString) {
  return z
    .string()
    .trim()
    .optional()
    .transform((value) => value || undefined)
    .pipe(schema.optional());
}

export const loginSchema = z.object({
  email: emailSchema,
  password: z.string().min(1, 'Enter your password.'),
});

export type LoginInput = z.infer<typeof loginSchema>;

export const registrationSchema = z.object({
  name: z
    .string()
    .trim()
    .min(1, 'Enter your name.')
    .max(NAME_MAX_LENGTH, 'Name is too long.'),
  email: emailSchema,
  password: passwordSchema,
  nif: optionalText(z.string().refine(isValidNif, 'Enter a valid NIF.')),
  contact: optionalText(
    z.string().max(CONTACT_MAX_LENGTH, 'Contact is too long.'),
  ),
  companyId: z.number({ error: 'Choose your company.' }),
  companyLocationId: z.number({ error: 'Choose your location.' }),
});

export type RegistrationInput = z.infer<typeof registrationSchema>;

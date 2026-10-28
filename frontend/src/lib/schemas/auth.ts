import * as z from 'zod';

const EMAIL_MAX_LENGTH = 320;

const emailSchema = z
  .string()
  .trim()
  .pipe(
    z
      .email('Enter a valid email address.')
      .max(EMAIL_MAX_LENGTH, 'Email is too long.'),
  );

export const loginSchema = z.object({
  email: emailSchema,
  password: z.string().min(1, 'Enter your password.'),
});

export type LoginInput = z.infer<typeof loginSchema>;

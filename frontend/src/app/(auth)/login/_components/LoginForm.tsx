'use client';

import { zodResolver } from '@hookform/resolvers/zod';
import { Loader2 } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { Controller, useForm } from 'react-hook-form';

import { Button } from '@/components/ui/button';
import { Field, FieldError, FieldLabel } from '@/components/ui/field';
import { Input } from '@/components/ui/input';
import type { ActionResult } from '@/lib/api/action-result';
import { errorMessage } from '@/lib/api/errors';
import { login } from '@/lib/auth/actions';
import { safeNext } from '@/lib/auth/safe-next';
import { type LoginInput, loginSchema } from '@/lib/schemas/auth';

const FIELDS = ['email', 'password'] as const satisfies (keyof LoginInput)[];

function isLoginField(field: string): field is keyof LoginInput {
  return (FIELDS as readonly string[]).includes(field);
}

export function LoginForm({ next }: { next?: string }) {
  const router = useRouter();
  const form = useForm<LoginInput>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: '', password: '' },
  });

  async function onSubmit(values: LoginInput) {
    let result: ActionResult;
    try {
      result = await login(values);
    } catch {
      form.setError('root.server', {
        message: errorMessage('INTERNAL_ERROR'),
      });
      return;
    }

    if (result.ok) {
      router.replace(safeNext(next));
      return;
    }

    let shownOnField = false;
    for (const [field, message] of Object.entries(
      result.error.fieldErrors ?? {},
    )) {
      if (isLoginField(field)) {
        form.setError(field, { message });
        shownOnField = true;
      }
    }
    if (!shownOnField) {
      form.setError('root.server', { message: result.error.message });
    }
  }

  const { isSubmitting, errors } = form.formState;

  return (
    <form
      noValidate
      onSubmit={form.handleSubmit(onSubmit)}
      className="space-y-4"
    >
      <Controller
        name="email"
        control={form.control}
        render={({ field, fieldState }) => (
          <Field data-invalid={fieldState.invalid}>
            <FieldLabel htmlFor={field.name}>Email</FieldLabel>
            <Input
              {...field}
              id={field.name}
              type="email"
              autoComplete="email"
              placeholder="ana.ferreira@empresa.pt"
              aria-invalid={fieldState.invalid}
              aria-describedby={
                fieldState.invalid ? `${field.name}-error` : undefined
              }
            />
            {fieldState.invalid && (
              <FieldError
                id={`${field.name}-error`}
                errors={[fieldState.error]}
              />
            )}
          </Field>
        )}
      />
      <Controller
        name="password"
        control={form.control}
        render={({ field, fieldState }) => (
          <Field data-invalid={fieldState.invalid}>
            <FieldLabel htmlFor={field.name}>Password</FieldLabel>
            <Input
              {...field}
              id={field.name}
              type="password"
              autoComplete="current-password"
              aria-invalid={fieldState.invalid}
              aria-describedby={
                fieldState.invalid ? `${field.name}-error` : undefined
              }
            />
            {fieldState.invalid && (
              <FieldError
                id={`${field.name}-error`}
                errors={[fieldState.error]}
              />
            )}
          </Field>
        )}
      />
      {errors.root?.server && (
        <FieldError>{errors.root.server.message}</FieldError>
      )}
      <Button type="submit" className="h-9 w-full" disabled={isSubmitting}>
        {isSubmitting && <Loader2 className="animate-spin" aria-hidden />}
        Sign in
      </Button>
    </form>
  );
}

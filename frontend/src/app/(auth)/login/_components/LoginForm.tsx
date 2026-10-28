'use client';

import { zodResolver } from '@hookform/resolvers/zod';
import { Loader2 } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useForm } from 'react-hook-form';

import { FormTextField } from '@/components/forms/FormTextField';
import { Button } from '@/components/ui/button';
import { FieldError } from '@/components/ui/field';
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
      <FormTextField
        control={form.control}
        name="email"
        label="Email"
        required
        type="email"
        autoComplete="email"
        placeholder="ana.ferreira@empresa.pt"
      />
      <FormTextField
        control={form.control}
        name="password"
        label="Password"
        required
        type="password"
        autoComplete="current-password"
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

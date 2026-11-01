'use client';

import { zodResolver } from '@hookform/resolvers/zod';
import { Loader2 } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';

import { FormTextField } from '@/components/forms/FormTextField';
import { RequiredMark } from '@/components/forms/RequiredMark';
import { Button } from '@/components/ui/button';
import { Field, FieldError, FieldLabel } from '@/components/ui/field';
import type { ActionResult } from '@/lib/api/action-result';
import { errorMessage } from '@/lib/api/errors';
import { register } from '@/lib/auth/actions';
import { type RegistrationInput, registrationSchema } from '@/lib/schemas/auth';
import { type Company, CompanyCombobox } from './CompanyCombobox';
import {
  type CompanyLocation,
  loadLocations,
  LocationSelect,
} from './LocationSelect';

const FIELDS = [
  'name',
  'email',
  'password',
  'nif',
  'contact',
  'companyId',
  'companyLocationId',
] as const satisfies (keyof RegistrationInput)[];

function isRegistrationField(field: string): field is keyof RegistrationInput {
  return (FIELDS as readonly string[]).includes(field);
}

export function RegisterForm() {
  const router = useRouter();
  const form = useForm<RegistrationInput>({
    resolver: zodResolver(registrationSchema),
    defaultValues: {
      name: '',
      email: '',
      password: '',
      nif: '',
      contact: '',
    },
  });
  const [locations, setLocations] = useState<Promise<
    CompanyLocation[] | null
  > | null>(null);

  function handleCompanyChange(company: Company) {
    form.setValue('companyId', company.id, { shouldValidate: true });
    form.resetField('companyLocationId');
    setLocations(loadLocations(company.id));
  }

  async function onSubmit(values: RegistrationInput) {
    let result: ActionResult;
    try {
      result = await register(values);
    } catch {
      form.setError('root.server', {
        message: errorMessage('INTERNAL_ERROR'),
      });
      return;
    }

    if (result.ok) {
      router.replace('/');
      return;
    }

    let shownOnField = false;
    for (const [field, message] of Object.entries(
      result.error.fieldErrors ?? {},
    )) {
      if (isRegistrationField(field)) {
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
        name="name"
        label="Full name"
        required
        autoComplete="name"
      />
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
        autoComplete="new-password"
        description="At least 8 characters."
      />
      <FormTextField
        control={form.control}
        name="nif"
        label="NIF"
        inputMode="numeric"
        autoComplete="off"
        maxLength={9}
      />
      <FormTextField
        control={form.control}
        name="contact"
        label="Contact"
        type="tel"
        autoComplete="tel"
      />
      <Controller
        control={form.control}
        name="companyId"
        render={({ field, fieldState }) => (
          <Field data-invalid={fieldState.invalid}>
            <FieldLabel htmlFor={field.name}>
              Company
              <RequiredMark />
            </FieldLabel>
            <CompanyCombobox
              id={field.name}
              ref={field.ref}
              onChange={handleCompanyChange}
              invalid={fieldState.invalid}
              describedBy={
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
        control={form.control}
        name="companyLocationId"
        render={({ field, fieldState }) => (
          <Field data-invalid={fieldState.invalid}>
            <FieldLabel htmlFor={field.name}>
              Location
              <RequiredMark />
            </FieldLabel>
            <LocationSelect
              id={field.name}
              ref={field.ref}
              locations={locations}
              value={field.value}
              onChange={field.onChange}
              invalid={fieldState.invalid}
              describedBy={
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
        Create account
      </Button>
    </form>
  );
}

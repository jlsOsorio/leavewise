'use client';

import { type Control, Controller } from 'react-hook-form';

import { RequiredMark } from '@/components/forms/RequiredMark';
import { Field, FieldError, FieldLabel } from '@/components/ui/field';
import { Input } from '@/components/ui/input';
import type { RegistrationInput } from '@/lib/schemas/auth';

export function PickerPlaceholder({
  control,
  name,
  label,
  placeholder,
}: {
  control: Control<RegistrationInput>;
  name: 'companyLocationId';
  label: string;
  placeholder: string;
}) {
  return (
    <Controller
      control={control}
      name={name}
      render={({ fieldState }) => (
        <Field data-invalid={fieldState.invalid}>
          <FieldLabel htmlFor={name}>
            {label}
            <RequiredMark />
          </FieldLabel>
          <Input
            id={name}
            disabled
            required
            placeholder={placeholder}
            aria-invalid={fieldState.invalid}
            aria-describedby={fieldState.invalid ? `${name}-error` : undefined}
          />
          {fieldState.invalid && (
            <FieldError id={`${name}-error`} errors={[fieldState.error]} />
          )}
        </Field>
      )}
    />
  );
}

'use client';

import type { ComponentProps } from 'react';
import {
  type Control,
  Controller,
  type FieldPath,
  type FieldValues,
} from 'react-hook-form';

import {
  Field,
  FieldDescription,
  FieldError,
  FieldLabel,
} from '@/components/ui/field';
import { Input } from '@/components/ui/input';
import { RequiredMark } from './RequiredMark';

type FormTextFieldProps<TValues extends FieldValues> = {
  control: Control<TValues>;
  name: FieldPath<TValues>;
  label: string;
  description?: string;
  required?: boolean;
} & Omit<ComponentProps<'input'>, 'name' | 'id' | 'value' | 'defaultValue'>;

export function FormTextField<TValues extends FieldValues>({
  control,
  name,
  label,
  description,
  required = false,
  ...inputProps
}: FormTextFieldProps<TValues>) {
  return (
    <Controller
      control={control}
      name={name}
      render={({ field, fieldState }) => {
        const descriptionId = description ? `${field.name}-description` : '';
        const errorId = fieldState.invalid ? `${field.name}-error` : '';
        return (
          <Field data-invalid={fieldState.invalid}>
            <FieldLabel htmlFor={field.name}>
              {label}
              {required && <RequiredMark />}
            </FieldLabel>
            <Input
              {...inputProps}
              {...field}
              id={field.name}
              required={required}
              aria-invalid={fieldState.invalid}
              aria-describedby={
                `${descriptionId} ${errorId}`.trim() || undefined
              }
            />
            {description && (
              <FieldDescription id={descriptionId}>
                {description}
              </FieldDescription>
            )}
            {fieldState.invalid && (
              <FieldError id={errorId} errors={[fieldState.error]} />
            )}
          </Field>
        );
      }}
    />
  );
}

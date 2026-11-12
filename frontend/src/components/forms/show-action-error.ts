import type { FieldPath, FieldValues, UseFormReturn } from 'react-hook-form';

import type { ActionError } from '@/lib/api/action-result';

export function showActionError<TValues extends FieldValues>(
  form: UseFormReturn<TValues>,
  error: ActionError,
  fields: readonly FieldPath<TValues>[],
): void {
  let shownOnField = false;
  for (const [field, message] of Object.entries(error.fieldErrors ?? {})) {
    if (isFormField(field, fields)) {
      form.setError(field, { message });
      shownOnField = true;
    }
  }
  if (!shownOnField) {
    form.setError('root.server', { message: error.message });
  }
}

function isFormField<TValues extends FieldValues>(
  field: string,
  fields: readonly FieldPath<TValues>[],
): field is FieldPath<TValues> {
  return (fields as readonly string[]).includes(field);
}

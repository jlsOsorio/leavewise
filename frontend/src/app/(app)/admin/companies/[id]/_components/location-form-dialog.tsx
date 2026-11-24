'use client';

import { zodResolver } from '@hookform/resolvers/zod';
import { Loader2 } from 'lucide-react';
import { type ReactElement, useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { toast } from 'sonner';

import { FormTextField } from '@/components/forms/form-text-field';
import { RequiredMark } from '@/components/forms/required-mark';
import { showActionError } from '@/components/forms/show-action-error';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog';
import { Field, FieldError, FieldLabel } from '@/components/ui/field';
import type { ActionResult } from '@/lib/api/action-result';
import { errorMessage } from '@/lib/api/errors';
import type { LocationDetails } from '@/lib/api/locations';
import { type LocationInput, locationSchema } from '@/lib/schemas/location';
import { createLocation, updateLocation } from '../actions';
import { MunicipalityCombobox } from './municipality-combobox';

const FIELDS = [
  'name',
  'address',
  'municipalityId',
] as const satisfies (keyof LocationInput)[];

type LocationFormDialogProps = {
  companyId: number;
  location?: LocationDetails;
  children: ReactElement;
};

export function LocationFormDialog({
  companyId,
  location,
  children,
}: LocationFormDialogProps) {
  const [open, setOpen] = useState(false);

  function handleSaved() {
    setOpen(false);
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>{children}</DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>
            {location ? 'Edit location' : 'New location'}
          </DialogTitle>
          <DialogDescription>
            The municipality decides which municipal holiday applies.
          </DialogDescription>
        </DialogHeader>
        <LocationForm
          companyId={companyId}
          location={location}
          onSaved={handleSaved}
        />
      </DialogContent>
    </Dialog>
  );
}

function LocationForm({
  companyId,
  location,
  onSaved,
}: {
  companyId: number;
  location?: LocationDetails;
  onSaved: () => void;
}) {
  const form = useForm<LocationInput>({
    resolver: zodResolver(locationSchema),
    defaultValues: {
      name: location?.name ?? '',
      address: location?.address ?? '',
      municipalityId: location?.municipality.id,
    },
  });

  async function onSubmit(values: LocationInput) {
    let result: ActionResult;
    try {
      result = location
        ? await updateLocation(companyId, location.id, values)
        : await createLocation(companyId, values);
    } catch {
      form.setError('root.server', {
        message: errorMessage('INTERNAL_ERROR'),
      });
      return;
    }

    if (result.ok) {
      toast.success(location ? 'Location updated.' : 'Location created.');
      onSaved();
      return;
    }
    showActionError(form, result.error, FIELDS);
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
        label="Name"
        required
        autoComplete="off"
      />
      <FormTextField
        control={form.control}
        name="address"
        label="Address"
        autoComplete="street-address"
      />
      <Controller
        control={form.control}
        name="municipalityId"
        render={({ field, fieldState }) => (
          <Field data-invalid={fieldState.invalid}>
            <FieldLabel htmlFor={field.name}>
              Municipality
              <RequiredMark />
            </FieldLabel>
            <MunicipalityCombobox
              id={field.name}
              ref={field.ref}
              defaultValue={location?.municipality}
              onChange={(municipality) => field.onChange(municipality.id)}
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
      <DialogFooter>
        <DialogClose asChild>
          <Button variant="outline">Cancel</Button>
        </DialogClose>
        <Button type="submit" disabled={isSubmitting}>
          {isSubmitting && <Loader2 className="animate-spin" aria-hidden />}
          Save
        </Button>
      </DialogFooter>
    </form>
  );
}

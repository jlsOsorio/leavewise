'use client';

import { zodResolver } from '@hookform/resolvers/zod';
import { Loader2 } from 'lucide-react';
import { type ReactElement, useState } from 'react';
import { useForm } from 'react-hook-form';
import { toast } from 'sonner';

import { FormTextField } from '@/components/forms/FormTextField';
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
import { FieldError } from '@/components/ui/field';
import type { ActionResult } from '@/lib/api/action-result';
import { errorMessage } from '@/lib/api/errors';
import type { Municipality } from '@/lib/api/municipalities';
import {
  type MunicipalityInput,
  municipalitySchema,
} from '@/lib/schemas/municipality';
import { createMunicipality, updateMunicipality } from '../actions';

const FIELDS = [
  'name',
  'district',
  'country',
] as const satisfies (keyof MunicipalityInput)[];
const DEFAULT_COUNTRY = 'Portugal';

type MunicipalityFormDialogProps = {
  municipality?: Municipality;
  // The element that opens the dialog
  children: ReactElement;
};

export function MunicipalityFormDialog({
  municipality,
  children,
}: MunicipalityFormDialogProps) {
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
            {municipality ? 'Edit municipality' : 'New municipality'}
          </DialogTitle>
          <DialogDescription>
            Locations and holidays refer to municipalities.
          </DialogDescription>
        </DialogHeader>
        <MunicipalityForm municipality={municipality} onSaved={handleSaved} />
      </DialogContent>
    </Dialog>
  );
}

function MunicipalityForm({
  municipality,
  onSaved,
}: {
  municipality?: Municipality;
  onSaved: () => void;
}) {
  const form = useForm<MunicipalityInput>({
    resolver: zodResolver(municipalitySchema),
    defaultValues: {
      name: municipality?.name ?? '',
      district: municipality?.district ?? '',
      country: municipality?.country ?? DEFAULT_COUNTRY,
    },
  });

  async function onSubmit(values: MunicipalityInput) {
    let result: ActionResult;
    try {
      result = municipality
        ? await updateMunicipality(municipality.id, values)
        : await createMunicipality(values);
    } catch {
      form.setError('root.server', {
        message: errorMessage('INTERNAL_ERROR'),
      });
      return;
    }

    if (result.ok) {
      toast.success(
        municipality ? 'Municipality updated.' : 'Municipality created.',
      );
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
        name="district"
        label="District"
        required
        autoComplete="off"
      />
      <FormTextField
        control={form.control}
        name="country"
        label="Country"
        description="Leave blank for Portugal."
        autoComplete="country-name"
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

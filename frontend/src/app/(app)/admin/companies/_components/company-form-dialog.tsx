'use client';

import { zodResolver } from '@hookform/resolvers/zod';
import { Loader2 } from 'lucide-react';
import { type ReactElement, useState } from 'react';
import { useForm } from 'react-hook-form';
import { toast } from 'sonner';

import { FormTextField } from '@/components/forms/form-text-field';
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
import type { Company } from '@/lib/api/companies';
import { errorMessage } from '@/lib/api/errors';
import { type CompanyInput, companySchema } from '@/lib/schemas/company';
import { createCompany, updateCompany } from '../actions';

const FIELDS = ['name'] as const satisfies (keyof CompanyInput)[];

type CompanyFormDialogProps = {
  company?: Company;
  children: ReactElement;
};

export function CompanyFormDialog({
  company,
  children,
}: CompanyFormDialogProps) {
  const [open, setOpen] = useState(false);

  function handleSaved() {
    setOpen(false);
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>{children}</DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{company ? 'Edit company' : 'New company'}</DialogTitle>
          <DialogDescription>
            Users pick their company when they sign up.
          </DialogDescription>
        </DialogHeader>
        <CompanyForm company={company} onSaved={handleSaved} />
      </DialogContent>
    </Dialog>
  );
}

function CompanyForm({
  company,
  onSaved,
}: {
  company?: Company;
  onSaved: () => void;
}) {
  const form = useForm<CompanyInput>({
    resolver: zodResolver(companySchema),
    defaultValues: { name: company?.name ?? '' },
  });

  async function onSubmit(values: CompanyInput) {
    let result: ActionResult;
    try {
      result = company
        ? await updateCompany(company.id, values)
        : await createCompany(values);
    } catch {
      form.setError('root.server', {
        message: errorMessage('INTERNAL_ERROR'),
      });
      return;
    }

    if (result.ok) {
      toast.success(company ? 'Company updated.' : 'Company created.');
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
        autoComplete="organization"
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

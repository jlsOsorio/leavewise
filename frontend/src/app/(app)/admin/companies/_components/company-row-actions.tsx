'use client';

import { Pencil, Trash2 } from 'lucide-react';

import { ConfirmDialog } from '@/components/forms/confirm-dialog';
import { Button } from '@/components/ui/button';
import type { Company } from '@/lib/api/companies';
import { deleteCompany } from '../actions';
import { CompanyFormDialog } from './company-form-dialog';

export function CompanyRowActions({ company }: { company: Company }) {
  function handleDelete() {
    return deleteCompany(company.id);
  }

  return (
    <div className="flex justify-end gap-1">
      <CompanyFormDialog company={company}>
        <Button
          variant="ghost"
          size="icon-sm"
          aria-label={`Edit ${company.name}`}
        >
          <Pencil aria-hidden />
        </Button>
      </CompanyFormDialog>
      <ConfirmDialog
        title={`Delete ${company.name}?`}
        description="This also deletes its locations, work schedule and vacation policies, and can't be undone. Companies with users can't be deleted."
        confirmLabel="Delete"
        successMessage="Company deleted."
        onConfirm={handleDelete}
      >
        <Button
          variant="ghost"
          size="icon-sm"
          aria-label={`Delete ${company.name}`}
        >
          <Trash2 aria-hidden />
        </Button>
      </ConfirmDialog>
    </div>
  );
}

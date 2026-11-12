'use client';

import { Pencil, Trash2 } from 'lucide-react';

import { ConfirmDialog } from '@/components/forms/ConfirmDialog';
import { Button } from '@/components/ui/button';
import type { Municipality } from '@/lib/api/municipalities';
import { deleteMunicipality } from '../actions';
import { MunicipalityFormDialog } from './MunicipalityFormDialog';

export function MunicipalityRowActions({
  municipality,
}: {
  municipality: Municipality;
}) {
  function handleDelete() {
    return deleteMunicipality(municipality.id);
  }

  return (
    <div className="flex justify-end gap-1">
      <MunicipalityFormDialog municipality={municipality}>
        <Button
          variant="ghost"
          size="icon-sm"
          aria-label={`Edit ${municipality.name}`}
        >
          <Pencil aria-hidden />
        </Button>
      </MunicipalityFormDialog>
      <ConfirmDialog
        title={`Delete ${municipality.name}?`}
        description="This can't be undone. Municipalities used by a location or holiday can't be deleted."
        confirmLabel="Delete"
        successMessage="Municipality deleted."
        onConfirm={handleDelete}
      >
        <Button
          variant="ghost"
          size="icon-sm"
          aria-label={`Delete ${municipality.name}`}
        >
          <Trash2 aria-hidden />
        </Button>
      </ConfirmDialog>
    </div>
  );
}

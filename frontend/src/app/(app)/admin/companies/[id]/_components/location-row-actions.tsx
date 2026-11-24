'use client';

import { Pencil, Trash2 } from 'lucide-react';

import { ConfirmDialog } from '@/components/forms/confirm-dialog';
import { Button } from '@/components/ui/button';
import type { LocationDetails } from '@/lib/api/locations';
import { deleteLocation } from '../actions';
import { LocationFormDialog } from './location-form-dialog';

export function LocationRowActions({
  location,
}: {
  location: LocationDetails;
}) {
  function handleDelete() {
    return deleteLocation(location.companyId, location.id);
  }

  return (
    <div className="flex justify-end gap-1">
      <LocationFormDialog companyId={location.companyId} location={location}>
        <Button
          variant="ghost"
          size="icon-sm"
          aria-label={`Edit ${location.name}`}
        >
          <Pencil aria-hidden />
        </Button>
      </LocationFormDialog>
      <ConfirmDialog
        title={`Delete ${location.name}?`}
        description="This can't be undone. Locations with users can't be deleted."
        confirmLabel="Delete"
        successMessage="Location deleted."
        onConfirm={handleDelete}
      >
        <Button
          variant="ghost"
          size="icon-sm"
          aria-label={`Delete ${location.name}`}
        >
          <Trash2 aria-hidden />
        </Button>
      </ConfirmDialog>
    </div>
  );
}

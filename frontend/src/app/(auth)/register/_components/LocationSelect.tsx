'use client';

import { type Ref, Suspense, use } from 'react';

import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Skeleton } from '@/components/ui/skeleton';
import type { Page } from '@/lib/api/page';

export type CompanyLocation = { id: number; name: string };

export async function loadLocations(
  companyId: number,
): Promise<CompanyLocation[] | null> {
  try {
    const response = await fetch(`/api/auth/companies/${companyId}/locations`);
    if (!response.ok) {
      return null;
    }
    return ((await response.json()) as Page<CompanyLocation>).content;
  } catch {
    return null;
  }
}

type LocationSelectProps = {
  id: string;
  ref?: Ref<HTMLButtonElement>;
  locations: Promise<CompanyLocation[] | null> | null;
  value: number | undefined;
  onChange: (locationId: number) => void;
  invalid: boolean;
  describedBy?: string;
};

type LocationOptionsProps = Omit<LocationSelectProps, 'locations'> & {
  options?: CompanyLocation[];
  placeholder: string;
};

export function LocationSelect({ locations, ...props }: LocationSelectProps) {
  if (!locations) {
    return <LocationOptions {...props} placeholder="Choose a company first" />;
  }
  return (
    <Suspense fallback={<Skeleton className="h-8 w-full" />}>
      <LoadedLocationSelect {...props} locations={locations} />
    </Suspense>
  );
}

function LoadedLocationSelect({
  locations,
  ...props
}: Omit<LocationSelectProps, 'locations'> & {
  locations: Promise<CompanyLocation[] | null>;
}) {
  const options = use(locations);
  if (!options) {
    return (
      <LocationOptions {...props} placeholder="Locations could not be loaded" />
    );
  }
  if (options.length === 0) {
    return (
      <LocationOptions {...props} placeholder="This company has no locations" />
    );
  }
  return (
    <LocationOptions
      {...props}
      options={options}
      placeholder="Choose your location"
    />
  );
}

function LocationOptions({
  id,
  ref,
  value,
  onChange,
  invalid,
  describedBy,
  options = [],
  placeholder,
}: LocationOptionsProps) {
  function handleValueChange(locationId: string) {
    onChange(Number(locationId));
  }

  return (
    <Select
      value={value === undefined ? '' : String(value)}
      onValueChange={handleValueChange}
      disabled={options.length === 0}
    >
      <SelectTrigger
        ref={ref}
        id={id}
        className="w-full"
        aria-required
        aria-invalid={invalid}
        aria-describedby={describedBy}
      >
        <SelectValue placeholder={placeholder} />
      </SelectTrigger>
      <SelectContent>
        {options.map((location) => (
          <SelectItem key={location.id} value={String(location.id)}>
            {location.name}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}

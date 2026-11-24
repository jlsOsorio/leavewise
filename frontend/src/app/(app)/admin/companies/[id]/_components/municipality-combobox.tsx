'use client';

import type { Ref } from 'react';

import {
  AsyncCombobox,
  type AsyncComboboxTexts,
} from '@/components/forms/async-combobox';
import type { Municipality } from '@/lib/api/municipalities';
import { municipalityLabel } from './municipality-label';

const TEXTS: AsyncComboboxTexts = {
  placeholder: 'Search for a municipality',
  searchPlaceholder: 'Type a municipality name',
  searching: 'Searching municipalities…',
  empty: 'No municipality matches that name.',
  failed: 'Municipalities could not be loaded. Try again.',
};

type MunicipalityComboboxProps = {
  id: string;
  ref?: Ref<HTMLButtonElement>;
  defaultValue?: Municipality;
  onChange: (municipality: Municipality) => void;
  invalid: boolean;
  describedBy?: string;
};

export function MunicipalityCombobox(props: MunicipalityComboboxProps) {
  return (
    <AsyncCombobox
      {...props}
      searchPath="/api/admin/municipalities"
      optionLabel={municipalityLabel}
      texts={TEXTS}
    />
  );
}

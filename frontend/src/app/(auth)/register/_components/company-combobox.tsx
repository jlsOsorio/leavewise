'use client';

import type { Ref } from 'react';

import {
  AsyncCombobox,
  type AsyncComboboxTexts,
} from '@/components/forms/async-combobox';
import type { Company } from '@/lib/api/companies';

const TEXTS: AsyncComboboxTexts = {
  placeholder: 'Search for your company',
  searchPlaceholder: 'Type a company name',
  searching: 'Searching companies…',
  empty: 'No company matches that name.',
  failed: 'Companies could not be loaded. Try again.',
};

type CompanyComboboxProps = {
  id: string;
  ref?: Ref<HTMLButtonElement>;
  onChange: (company: Company) => void;
  invalid: boolean;
  describedBy?: string;
};

export function CompanyCombobox(props: CompanyComboboxProps) {
  return (
    <AsyncCombobox
      {...props}
      searchPath="/api/auth/companies"
      optionLabel={companyName}
      texts={TEXTS}
    />
  );
}

function companyName(company: Company): string {
  return company.name;
}

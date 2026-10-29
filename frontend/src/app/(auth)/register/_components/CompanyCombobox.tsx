'use client';

import { Check, ChevronsUpDown } from 'lucide-react';
import { type Ref, useRef, useState } from 'react';
import { cn } from 'cn';

import { Button } from '@/components/ui/button';
import {
  Command,
  CommandEmpty,
  CommandGroup,
  CommandInput,
  CommandItem,
  CommandList,
} from '@/components/ui/command';
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from '@/components/ui/popover';
import { Skeleton } from '@/components/ui/skeleton';
import type { Page } from '@/lib/api/page';

export type Company = { id: number; name: string };

const SEARCH_DEBOUNCE_MS = 300;

type SearchState =
  | { status: 'idle' }
  | { status: 'loading' }
  | { status: 'failed' }
  | { status: 'done'; companies: Company[]; total: number };

type CompanyComboboxProps = {
  id: string;
  ref?: Ref<HTMLButtonElement>;
  onChange: (company: Company) => void;
  invalid: boolean;
  describedBy?: string;
};

export function CompanyCombobox({
  id,
  ref,
  onChange,
  invalid,
  describedBy,
}: CompanyComboboxProps) {
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState('');
  const [selected, setSelected] = useState<Company>();
  const [search, setSearch] = useState<SearchState>({ status: 'idle' });
  const debounceTimer = useRef<ReturnType<typeof setTimeout>>(undefined);
  const inFlight = useRef<AbortController>(undefined);

  async function runSearch(q: string) {
    inFlight.current?.abort();
    const controller = new AbortController();
    inFlight.current = controller;
    setSearch({ status: 'loading' });

    try {
      const response = await fetch(
        `/api/auth/companies?q=${encodeURIComponent(q)}`,
        { signal: controller.signal },
      );
      if (!response.ok) {
        throw new Error(`Company search failed: ${response.status}`);
      }
      const page = (await response.json()) as Page<Company>;
      setSearch({
        status: 'done',
        companies: page.content,
        total: page.totalElements,
      });
    } catch {
      if (!controller.signal.aborted) {
        setSearch({ status: 'failed' });
      }
    }
  }

  function handleOpenChange(nextOpen: boolean) {
    setOpen(nextOpen);
    if (nextOpen && search.status === 'idle') {
      void runSearch(query);
    }
  }

  function handleQueryChange(nextQuery: string) {
    setQuery(nextQuery);
    clearTimeout(debounceTimer.current);
    debounceTimer.current = setTimeout(
      () => void runSearch(nextQuery.trim()),
      SEARCH_DEBOUNCE_MS,
    );
  }

  function handleSelect(company: Company) {
    setSelected(company);
    setOpen(false);
    onChange(company);
  }

  return (
    <Popover open={open} onOpenChange={handleOpenChange}>
      <PopoverTrigger asChild>
        <Button
          ref={ref}
          id={id}
          type="button"
          variant="outline"
          role="combobox"
          aria-expanded={open}
          aria-required
          aria-invalid={invalid}
          aria-describedby={describedBy}
          className="w-full justify-between font-normal"
        >
          {selected ? (
            <span className="truncate">{selected.name}</span>
          ) : (
            <span className="text-muted-foreground">
              Search for your company
            </span>
          )}
          <ChevronsUpDown className="opacity-50" aria-hidden />
        </Button>
      </PopoverTrigger>
      <PopoverContent
        align="start"
        className="w-(--radix-popover-trigger-width) p-0"
      >
        <Command shouldFilter={false}>
          <CommandInput
            placeholder="Type a company name"
            value={query}
            onValueChange={handleQueryChange}
          />
          <CommandList>
            <SearchResults
              search={search}
              selectedId={selected?.id}
              onSelect={handleSelect}
            />
          </CommandList>
        </Command>
      </PopoverContent>
    </Popover>
  );
}

function SearchResults({
  search,
  selectedId,
  onSelect,
}: {
  search: SearchState;
  selectedId: number | undefined;
  onSelect: (company: Company) => void;
}) {
  if (search.status === 'idle' || search.status === 'loading') {
    return (
      <div role="status" className="space-y-2 p-2">
        <span className="sr-only">Searching companies…</span>
        {[0, 1, 2].map((i) => (
          <Skeleton key={i} className="h-6 w-full" />
        ))}
      </div>
    );
  }
  if (search.status === 'failed') {
    return (
      <p role="alert" className="p-4 text-center text-sm text-destructive">
        Companies could not be loaded. Try again.
      </p>
    );
  }

  return (
    <>
      <CommandEmpty>No company matches that name.</CommandEmpty>
      <CommandGroup>
        {search.companies.map((company) => (
          <CommandItem
            key={company.id}
            value={String(company.id)}
            onSelect={() => onSelect(company)}
            className={cn(
              'hover:bg-foreground/6',
              // The chosen company keeps the hover shade (also when cmdk highlights it) and the
              // default cursor, so it reads as the current choice rather than a link
              company.id === selectedId
                ? 'bg-foreground/6 data-selected:bg-foreground/6'
                : 'cursor-pointer',
            )}
          >
            {company.name}
            {company.id === selectedId && (
              <Check aria-hidden className="text-primary!" />
            )}
          </CommandItem>
        ))}
      </CommandGroup>
      {search.total > search.companies.length && (
        <p className="px-3 py-2 text-xs text-muted-foreground">
          Showing {search.companies.length} of {search.total}. Keep typing to
          narrow the list.
        </p>
      )}
    </>
  );
}

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

const SEARCH_DEBOUNCE_MS = 300;

export type ComboboxOption = { id: number };

export type AsyncComboboxTexts = {
  placeholder: string;
  searchPlaceholder: string;
  searching: string;
  empty: string;
  failed: string;
};

type SearchState<T> =
  | { status: 'idle' }
  | { status: 'loading' }
  | { status: 'failed' }
  | { status: 'done'; options: T[]; total: number };

type AsyncComboboxProps<T extends ComboboxOption> = {
  id: string;
  ref?: Ref<HTMLButtonElement>;
  searchPath: string;
  optionLabel: (option: T) => string;
  texts: AsyncComboboxTexts;
  defaultValue?: T;
  onChange: (option: T) => void;
  invalid: boolean;
  describedBy?: string;
};

export function AsyncCombobox<T extends ComboboxOption>({
  id,
  ref,
  searchPath,
  optionLabel,
  texts,
  defaultValue,
  onChange,
  invalid,
  describedBy,
}: AsyncComboboxProps<T>) {
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState('');
  const [selected, setSelected] = useState<T | undefined>(defaultValue);
  const [search, setSearch] = useState<SearchState<T>>({ status: 'idle' });
  const debounceTimer = useRef<ReturnType<typeof setTimeout>>(undefined);
  const inFlight = useRef<AbortController>(undefined);

  async function runSearch(q: string) {
    inFlight.current?.abort();
    const controller = new AbortController();
    inFlight.current = controller;
    setSearch({ status: 'loading' });

    try {
      const response = await fetch(`${searchPath}?q=${encodeURIComponent(q)}`, {
        signal: controller.signal,
      });
      if (!response.ok) {
        throw new Error(`Search failed: ${response.status}`);
      }
      const page = (await response.json()) as Page<T>;
      setSearch({
        status: 'done',
        options: page.content,
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

  function handleSelect(option: T) {
    setSelected(option);
    setOpen(false);
    onChange(option);
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
            <span className="truncate">{optionLabel(selected)}</span>
          ) : (
            <span className="text-muted-foreground">{texts.placeholder}</span>
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
            placeholder={texts.searchPlaceholder}
            value={query}
            onValueChange={handleQueryChange}
          />
          <CommandList>
            <SearchResults
              search={search}
              texts={texts}
              optionLabel={optionLabel}
              selectedId={selected?.id}
              onSelect={handleSelect}
            />
          </CommandList>
          {search.status === 'done' && search.total > search.options.length && (
            <p className="border-t px-3 py-2 text-xs text-muted-foreground">
              Showing {search.options.length} of {search.total}. Keep typing to
              narrow the list.
            </p>
          )}
        </Command>
      </PopoverContent>
    </Popover>
  );
}

function SearchResults<T extends ComboboxOption>({
  search,
  texts,
  optionLabel,
  selectedId,
  onSelect,
}: {
  search: SearchState<T>;
  texts: AsyncComboboxTexts;
  optionLabel: (option: T) => string;
  selectedId: number | undefined;
  onSelect: (option: T) => void;
}) {
  if (search.status === 'idle' || search.status === 'loading') {
    return (
      <div role="status" className="space-y-2 p-2">
        <span className="sr-only">{texts.searching}</span>
        {[0, 1, 2].map((i) => (
          <Skeleton key={i} className="h-6 w-full" />
        ))}
      </div>
    );
  }
  if (search.status === 'failed') {
    return (
      <p role="alert" className="p-4 text-center text-sm text-destructive">
        {texts.failed}
      </p>
    );
  }

  return (
    <>
      <CommandEmpty>{texts.empty}</CommandEmpty>
      <CommandGroup>
        {search.options.map((option) => (
          <CommandItem
            key={option.id}
            value={String(option.id)}
            onSelect={() => onSelect(option)}
            className={cn(
              'hover:bg-foreground/6',
              option.id === selectedId
                ? 'bg-foreground/6 data-selected:bg-foreground/6'
                : 'cursor-pointer',
            )}
          >
            {optionLabel(option)}
            {option.id === selectedId && (
              <Check aria-hidden className="text-primary!" />
            )}
          </CommandItem>
        ))}
      </CommandGroup>
    </>
  );
}

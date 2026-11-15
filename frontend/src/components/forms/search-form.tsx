import { Search } from 'lucide-react';
import Form from 'next/form';
import { useId } from 'react';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';

type SearchFormProps = {
  action: string;
  label: string;
  query: string;
  maxLength: number;
};

export function SearchForm({
  action,
  label,
  query,
  maxLength,
}: SearchFormProps) {
  const inputId = useId();

  return (
    <Form action={action} role="search" className="mb-4 flex max-w-sm gap-2">
      <label htmlFor={inputId} className="sr-only">
        {label}
      </label>
      <Input
        id={inputId}
        type="search"
        name="q"
        defaultValue={query}
        maxLength={maxLength}
        placeholder="Search by name"
      />
      <Button type="submit" variant="outline">
        <Search aria-hidden />
        Search
      </Button>
    </Form>
  );
}

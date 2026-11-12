import { Search } from 'lucide-react';
import Form from 'next/form';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { MUNICIPALITY_NAME_MAX_LENGTH } from '@/lib/schemas/municipality';

export function MunicipalitySearch({ query }: { query: string }) {
  return (
    <Form
      action="/admin/municipalities"
      role="search"
      className="mb-4 flex max-w-sm gap-2"
    >
      <label htmlFor="municipality-search" className="sr-only">
        Search municipalities by name
      </label>
      <Input
        id="municipality-search"
        type="search"
        name="q"
        defaultValue={query}
        maxLength={MUNICIPALITY_NAME_MAX_LENGTH}
        placeholder="Search by name"
      />
      <Button type="submit" variant="outline">
        <Search aria-hidden />
        Search
      </Button>
    </Form>
  );
}

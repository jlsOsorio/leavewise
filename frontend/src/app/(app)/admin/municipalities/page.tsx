import { Plus } from 'lucide-react';
import type { Metadata } from 'next';

import { type Column, DataTable } from '@/components/layout/DataTable';
import { PageHeader } from '@/components/layout/PageHeader';
import { Button } from '@/components/ui/button';
import type { Municipality } from '@/lib/api/municipalities';
import type { Page } from '@/lib/api/page';
import { serverApi } from '@/lib/api/server';
import { MUNICIPALITY_NAME_MAX_LENGTH } from '@/lib/schemas/municipality';
import { pageParam, textParam } from '@/lib/search-params';
import { MunicipalityFormDialog } from './_components/MunicipalityFormDialog';
import { MunicipalityRowActions } from './_components/MunicipalityRowActions';
import { MunicipalitySearch } from './_components/MunicipalitySearch';

export const metadata: Metadata = { title: 'Municipalities' };

const PAGE_PATH = '/admin/municipalities';

const COLUMNS: Column<Municipality>[] = [
  { key: 'name', header: 'Name', cell: (row) => row.name },
  { key: 'district', header: 'District', cell: (row) => row.district },
  { key: 'country', header: 'Country', cell: (row) => row.country },
  {
    key: 'actions',
    header: 'Actions',
    align: 'right',
    cell: (row) => <MunicipalityRowActions municipality={row} />,
  },
];

export default async function MunicipalitiesPage({
  searchParams,
}: PageProps<'/admin/municipalities'>) {
  const params = await searchParams;
  const query = textParam(params.q, MUNICIPALITY_NAME_MAX_LENGTH);
  const page = pageParam(params.page);

  const municipalities = await serverApi<Page<Municipality>>(
    '/municipalities',
    { query: { q: query || undefined, page, sort: 'name,asc' } },
  );

  function pageHref(target: number): string {
    const search = new URLSearchParams({ page: String(target) });
    if (query) {
      search.set('q', query);
    }
    return `${PAGE_PATH}?${search}`;
  }

  return (
    <>
      <PageHeader
        title="Municipalities"
        description="Where company locations are, which decides their municipal holidays."
        actions={
          <MunicipalityFormDialog>
            <Button>
              <Plus aria-hidden />
              New municipality
            </Button>
          </MunicipalityFormDialog>
        }
      />
      <MunicipalitySearch query={query} />
      <DataTable
        caption="Municipalities"
        columns={COLUMNS}
        rows={municipalities.content}
        rowKey={(row) => row.id}
        pageInfo={municipalities}
        pageHref={pageHref}
        empty={
          <p className="text-center text-muted-foreground">
            {query
              ? `No municipalities match "${query}".`
              : 'No municipalities yet.'}
          </p>
        }
      />
    </>
  );
}

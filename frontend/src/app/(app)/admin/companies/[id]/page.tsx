import { ArrowLeft, Plus } from 'lucide-react';
import type { Metadata } from 'next';
import Link from 'next/link';
import { notFound } from 'next/navigation';

import { type Column, DataTable } from '@/components/layout/data-table';
import { PageHeader } from '@/components/layout/page-header';
import { Button } from '@/components/ui/button';
import { ApiError } from '@/lib/api/api-error';
import type { Company } from '@/lib/api/companies';
import type { LocationDetails } from '@/lib/api/locations';
import { resourceIdSchema } from '@/lib/api/mutate';
import type { Page } from '@/lib/api/page';
import { serverApi } from '@/lib/api/server';
import { pageParam } from '@/lib/search-params';
import { LocationFormDialog } from './_components/location-form-dialog';
import { LocationRowActions } from './_components/location-row-actions';
import { municipalityLabel } from './_components/municipality-label';

export const metadata: Metadata = { title: 'Company locations' };

const COMPANIES_PATH = '/admin/companies';

const COLUMNS: Column<LocationDetails>[] = [
  { key: 'name', header: 'Name', cell: (row) => row.name },
  { key: 'address', header: 'Address', cell: (row) => row.address ?? '—' },
  {
    key: 'municipality',
    header: 'Municipality',
    cell: (row) => municipalityLabel(row.municipality),
  },
  {
    key: 'actions',
    header: 'Actions',
    align: 'right',
    cell: (row) => <LocationRowActions location={row} />,
  },
];

export default async function CompanyLocationsPage({
  params,
  searchParams,
}: PageProps<'/admin/companies/[id]'>) {
  const parsedId = resourceIdSchema.safeParse(Number((await params).id));
  if (!parsedId.success) {
    notFound();
  }
  const companyId = parsedId.data;
  const page = pageParam((await searchParams).page);

  const [company, locations] = await orNotFound(
    Promise.all([
      serverApi<Company>(`/companies/${companyId}`),
      serverApi<Page<LocationDetails>>(`/companies/${companyId}/locations`, {
        query: { page, sort: 'name,asc' },
      }),
    ]),
  );

  function pageHref(target: number): string {
    return `${COMPANIES_PATH}/${companyId}?page=${target}`;
  }

  return (
    <>
      <Link
        href={COMPANIES_PATH}
        className="mb-4 inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
      >
        <ArrowLeft className="size-4" aria-hidden />
        Companies
      </Link>
      <PageHeader
        title={company.name}
        description="The company's locations and the municipality each one is in."
        actions={
          <LocationFormDialog companyId={companyId}>
            <Button>
              <Plus aria-hidden />
              New location
            </Button>
          </LocationFormDialog>
        }
      />
      <DataTable
        caption={`Locations of ${company.name}`}
        columns={COLUMNS}
        rows={locations.content}
        rowKey={(row) => row.id}
        pageInfo={locations}
        pageHref={pageHref}
        empty={
          <p className="text-center text-muted-foreground">No locations yet.</p>
        }
      />
    </>
  );
}

async function orNotFound<T>(request: Promise<T>): Promise<T> {
  try {
    return await request;
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) {
      notFound();
    }
    throw error;
  }
}

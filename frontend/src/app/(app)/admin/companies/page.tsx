import { Plus } from 'lucide-react';
import type { Metadata } from 'next';
import Link from 'next/link';

import { SearchForm } from '@/components/forms/search-form';
import { type Column, DataTable } from '@/components/layout/data-table';
import { PageHeader } from '@/components/layout/page-header';
import { Button } from '@/components/ui/button';
import type { Company } from '@/lib/api/companies';
import type { Page } from '@/lib/api/page';
import { serverApi } from '@/lib/api/server';
import { COMPANY_NAME_MAX_LENGTH } from '@/lib/schemas/company';
import { pageParam, textParam } from '@/lib/search-params';
import { CompanyFormDialog } from './_components/company-form-dialog';
import { CompanyRowActions } from './_components/company-row-actions';

export const metadata: Metadata = { title: 'Companies' };

const PAGE_PATH = '/admin/companies';

const COLUMNS: Column<Company>[] = [
  {
    key: 'name',
    header: 'Name',
    cell: (row) => (
      <Link
        href={`${PAGE_PATH}/${row.id}`}
        className="underline-offset-4 hover:underline"
      >
        {row.name}
      </Link>
    ),
  },
  {
    key: 'actions',
    header: 'Actions',
    align: 'right',
    cell: (row) => <CompanyRowActions company={row} />,
  },
];

export default async function CompaniesPage({
  searchParams,
}: PageProps<'/admin/companies'>) {
  const params = await searchParams;
  const query = textParam(params.q, COMPANY_NAME_MAX_LENGTH);
  const page = pageParam(params.page);

  const companies = await serverApi<Page<Company>>('/companies', {
    query: { q: query || undefined, page, sort: 'name,asc' },
  });

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
        title="Companies"
        description="The companies users belong to, with their locations."
        actions={
          <CompanyFormDialog>
            <Button>
              <Plus aria-hidden />
              New company
            </Button>
          </CompanyFormDialog>
        }
      />
      <SearchForm
        action={PAGE_PATH}
        label="Search companies by name"
        query={query}
        maxLength={COMPANY_NAME_MAX_LENGTH}
      />
      <DataTable
        caption="Companies"
        columns={COLUMNS}
        rows={companies.content}
        rowKey={(row) => row.id}
        pageInfo={companies}
        pageHref={pageHref}
        empty={
          <p className="text-center text-muted-foreground">
            {query ? `No companies match "${query}".` : 'No companies yet.'}
          </p>
        }
      />
    </>
  );
}

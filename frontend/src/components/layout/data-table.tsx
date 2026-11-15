import { ChevronLeft, ChevronRight } from 'lucide-react';
import Link from 'next/link';
import type { ReactNode } from 'react';

import { Button } from '@/components/ui/button';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { cn } from '@/lib/utils';

export type Column<T> = {
  key: string;
  header: string;
  cell: (row: T) => ReactNode;
  align?: 'left' | 'right';
  className?: string;
};

export type PageInfo = {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

type DataTableProps<T> = {
  columns: Column<T>[];
  rows: T[];
  rowKey: (row: T) => string | number;
  pageInfo: PageInfo;
  pageHref: (page: number) => string;
  empty: ReactNode;
  caption?: string;
};

export function DataTable<T>({
  columns,
  rows,
  rowKey,
  pageInfo,
  pageHref,
  empty,
  caption,
}: DataTableProps<T>) {
  const { page, size, totalElements, totalPages } = pageInfo;
  const from = totalElements === 0 ? 0 : page * size + 1;
  const to = Math.min((page + 1) * size, totalElements);

  return (
    <div className="overflow-hidden rounded-lg border bg-card">
      <Table>
        {caption && <caption className="sr-only">{caption}</caption>}
        <TableHeader>
          <TableRow className="hover:bg-transparent">
            {columns.map((col) => (
              <TableHead
                key={col.key}
                className={cn(
                  'h-10 px-4 text-xs font-medium text-muted-foreground',
                  col.align === 'right' && 'text-right',
                  col.className,
                )}
              >
                {col.header}
              </TableHead>
            ))}
          </TableRow>
        </TableHeader>
        <TableBody>
          {rows.length === 0 ? (
            <TableRow className="hover:bg-transparent">
              <TableCell colSpan={columns.length} className="px-4 py-12">
                {empty}
              </TableCell>
            </TableRow>
          ) : (
            rows.map((row) => (
              <TableRow key={rowKey(row)}>
                {columns.map((col) => (
                  <TableCell
                    key={col.key}
                    className={cn(
                      'h-11 px-4',
                      col.align === 'right' && 'text-right num',
                      col.className,
                    )}
                  >
                    {col.cell(row)}
                  </TableCell>
                ))}
              </TableRow>
            ))
          )}
        </TableBody>
      </Table>
      <nav
        aria-label="Pagination"
        className="flex items-center justify-between gap-4 border-t px-4 py-2.5 text-xs text-muted-foreground"
      >
        <span className="num">
          {from}–{to} of {totalElements}
        </span>
        <div className="flex items-center gap-1">
          <PagerButton
            href={page > 0 ? pageHref(page - 1) : undefined}
            label="Previous page"
          >
            <ChevronLeft />
          </PagerButton>
          <span className="px-2 num">
            Page {totalPages === 0 ? 0 : page + 1} of {totalPages}
          </span>
          <PagerButton
            href={page + 1 < totalPages ? pageHref(page + 1) : undefined}
            label="Next page"
          >
            <ChevronRight />
          </PagerButton>
        </div>
      </nav>
    </div>
  );
}

function PagerButton({
  href,
  label,
  children,
}: {
  href?: string;
  label: string;
  children: ReactNode;
}) {
  if (!href) {
    return (
      <Button variant="ghost" size="icon-xs" disabled aria-label={label}>
        {children}
      </Button>
    );
  }
  return (
    <Button variant="ghost" size="icon-xs" asChild>
      <Link href={href} aria-label={label}>
        {children}
      </Link>
    </Button>
  );
}

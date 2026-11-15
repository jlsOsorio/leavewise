import { TableSkeleton } from '@/components/layout/table-skeleton';
import { Skeleton } from '@/components/ui/skeleton';

export function CompaniesPageSkeleton() {
  return (
    <div aria-hidden>
      <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
        <div className="space-y-2">
          <Skeleton className="h-8 w-40" />
          <Skeleton className="h-5 w-96 max-w-full" />
        </div>
        <Skeleton className="h-8 w-36" />
      </div>
      <Skeleton className="mb-4 h-8 w-full max-w-sm" />
      <TableSkeleton columns={2} />
    </div>
  );
}

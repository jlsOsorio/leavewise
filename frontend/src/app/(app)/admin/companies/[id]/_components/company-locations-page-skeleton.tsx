import { TableSkeleton } from '@/components/layout/table-skeleton';
import { Skeleton } from '@/components/ui/skeleton';

export function CompanyLocationsPageSkeleton() {
  return (
    <div aria-hidden>
      <Skeleton className="mb-4 h-5 w-24" />
      <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
        <div className="space-y-2">
          <Skeleton className="h-8 w-56" />
          <Skeleton className="h-5 w-96 max-w-full" />
        </div>
        <Skeleton className="h-8 w-36" />
      </div>
      <TableSkeleton columns={4} />
    </div>
  );
}

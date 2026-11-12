import { TableSkeleton } from '@/components/layout/TableSkeleton';
import { Skeleton } from '@/components/ui/skeleton';

export function MunicipalitiesPageSkeleton() {
  return (
    <div aria-hidden>
      <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
        <div className="space-y-2">
          <Skeleton className="h-8 w-48" />
          <Skeleton className="h-5 w-96 max-w-full" />
        </div>
        <Skeleton className="h-8 w-40" />
      </div>
      <Skeleton className="mb-4 h-8 w-full max-w-sm" />
      <TableSkeleton columns={4} />
    </div>
  );
}

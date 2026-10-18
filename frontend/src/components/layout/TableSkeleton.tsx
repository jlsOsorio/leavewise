import { Skeleton } from '@/components/ui/skeleton';

const WIDTHS = ['w-32', 'w-40', 'w-24', 'w-16', 'w-20', 'w-28'];

export function TableSkeleton({
  columns,
  rows = 8,
}: {
  columns: number;
  rows?: number;
}) {
  return (
    <div aria-hidden className="overflow-hidden rounded-lg border bg-card">
      <div className="flex h-10 items-center gap-6 border-b px-4">
        {Array.from({ length: columns }, (_, c) => (
          <Skeleton key={c} className="h-3 w-16 flex-1" />
        ))}
      </div>
      {Array.from({ length: rows }, (_, r) => (
        <div
          key={r}
          className="flex h-11 items-center gap-6 border-b px-4 last:border-b-0"
        >
          {Array.from({ length: columns }, (_, c) => (
            <div key={c} className="flex-1">
              <Skeleton
                className={`h-3.5 ${WIDTHS[(r + c) % WIDTHS.length]} max-w-full`}
              />
            </div>
          ))}
        </div>
      ))}
      <div className="flex h-10 items-center justify-between border-t px-4">
        <Skeleton className="h-3 w-20" />
        <Skeleton className="h-3 w-24" />
      </div>
    </div>
  );
}

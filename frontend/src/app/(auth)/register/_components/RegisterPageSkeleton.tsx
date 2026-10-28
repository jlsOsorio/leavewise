import { Skeleton } from '@/components/ui/skeleton';

export function RegisterPageSkeleton() {
  return (
    <div role="status" className="space-y-6">
      <span className="sr-only">Loading…</span>
      <div className="space-y-2">
        <Skeleton className="h-7 w-48" />
        <Skeleton className="h-4 w-64" />
        <Skeleton className="h-3 w-44" />
      </div>
      <div className="space-y-4">
        {Array.from({ length: 7 }, (_, i) => (
          <div key={i} className="space-y-2">
            <Skeleton className="h-4 w-24" />
            <Skeleton className="h-8 w-full" />
          </div>
        ))}
        <Skeleton className="h-9 w-full" />
      </div>
      <Skeleton className="h-4 w-52" />
    </div>
  );
}

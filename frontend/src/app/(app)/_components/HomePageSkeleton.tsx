import { Skeleton } from '@/components/ui/skeleton';

export function HomePageSkeleton() {
  return (
    <div className="space-y-3">
      <Skeleton className="h-9 w-40" />
      <Skeleton className="h-5 w-80 max-w-full" />
    </div>
  );
}

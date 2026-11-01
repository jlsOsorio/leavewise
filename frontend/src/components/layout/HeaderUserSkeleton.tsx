import { Skeleton } from '@/components/ui/skeleton';

export function HeaderUserSkeleton() {
  return (
    <div className="flex items-center gap-2.5">
      <span className="flex flex-col items-end gap-1.5">
        <Skeleton className="h-3 w-24" />
        <Skeleton className="h-2.5 w-16" />
      </span>
      <Skeleton className="size-8 rounded-full" />
    </div>
  );
}

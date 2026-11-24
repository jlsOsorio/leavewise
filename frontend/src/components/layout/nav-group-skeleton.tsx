import { Skeleton } from '@/components/ui/skeleton';
import { NAV, type NavSection } from './nav';

export function NavGroupSkeleton({ section }: { section: NavSection }) {
  return (
    <div aria-hidden className="space-y-1 p-2">
      {NAV[section].map((item) => (
        <Skeleton key={item.href} className="h-8 w-full" />
      ))}
    </div>
  );
}

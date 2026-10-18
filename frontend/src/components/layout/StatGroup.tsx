import type { ReactNode } from 'react';

import { Skeleton } from '@/components/ui/skeleton';
import { cn } from '@/lib/utils';

/** One panel split by hairlines; stats compare against each other, so they share a frame. */
export function StatGroup({
  children,
  footer,
  className,
}: {
  children: ReactNode;
  footer?: ReactNode;
  className?: string;
}) {
  return (
    <section
      className={cn('overflow-hidden rounded-lg border bg-card', className)}
    >
      <dl className="grid grid-cols-2 divide-border sm:grid-cols-3 lg:grid-cols-5 lg:divide-x [&>*]:border-b lg:[&>*]:border-b-0">
        {children}
      </dl>
      {footer && <div className="border-t px-5 py-4">{footer}</div>}
    </section>
  );
}

type StatProps = {
  label: string;
  value: number | string;
  unit?: string;
  hint?: string;
  emphasis?: boolean;
};

export function Stat({ label, value, unit, hint, emphasis }: StatProps) {
  return (
    <div className="flex flex-col gap-1 px-5 py-4">
      <dt className="text-[13px] text-muted-foreground">{label}</dt>
      <dd className="flex items-baseline gap-1.5">
        <span
          className={cn(
            'text-[31px] leading-none font-semibold num',
            emphasis && 'text-primary',
          )}
        >
          {value}
        </span>
        {unit && <span className="text-muted-foreground">{unit}</span>}
      </dd>
      {hint && <dd className="text-xs text-muted-foreground">{hint}</dd>}
    </div>
  );
}

export function StatGroupSkeleton({ count = 5 }: { count?: number }) {
  return (
    <div aria-hidden className="overflow-hidden rounded-lg border bg-card">
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 lg:divide-x">
        {Array.from({ length: count }, (_, i) => (
          <div key={i} className="flex flex-col gap-2.5 px-5 py-4">
            <Skeleton className="h-3.5 w-20" />
            <Skeleton className="h-7 w-14" />
            <Skeleton className="h-3 w-24" />
          </div>
        ))}
      </div>
      <div className="border-t px-5 py-4">
        <Skeleton className="h-2 w-full" />
      </div>
    </div>
  );
}

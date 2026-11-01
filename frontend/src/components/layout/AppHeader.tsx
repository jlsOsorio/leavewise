import Link from 'next/link';
import { Suspense } from 'react';

import { HeaderUser } from './HeaderUser';
import { HeaderUserBoundary } from './HeaderUserBoundary';
import { HeaderUserSkeleton } from './HeaderUserSkeleton';

export function AppHeader() {
  return (
    <header className="flex h-14 items-center gap-3 border-b px-4 md:px-6">
      <Link
        href="/"
        className="flex items-center gap-2 text-[15px] font-semibold tracking-tight text-foreground"
      >
        <span
          aria-hidden
          className="grid size-6 grid-cols-2 gap-0.5 rounded-sm p-1"
        >
          <span className="rounded-[1px] bg-muted-foreground/50" />
          <span className="rounded-[1px] bg-muted-foreground/50" />
          <span className="rounded-[1px] bg-primary" />
          <span className="rounded-[1px] bg-muted-foreground/50" />
        </span>
        LeaveWise
      </Link>
      <div className="ml-auto">
        <HeaderUserBoundary>
          <Suspense fallback={<HeaderUserSkeleton />}>
            <HeaderUser />
          </Suspense>
        </HeaderUserBoundary>
      </div>
    </header>
  );
}

import Link from 'next/link';
import type { ReactNode } from 'react';
import { Suspense } from 'react';

import {
  Sidebar,
  SidebarContent,
  SidebarHeader,
  SidebarInset,
  SidebarProvider,
  SidebarTrigger,
} from '@/components/ui/sidebar';
import { TooltipProvider } from '@/components/ui/tooltip';
import { AdminNav } from './AdminNav';
import { AdminNavBoundary } from './AdminNavBoundary';
import { HeaderUser } from './HeaderUser';
import { HeaderUserBoundary } from './HeaderUserBoundary';
import { HeaderUserSkeleton } from './HeaderUserSkeleton';
import { NavGroup } from './NavGroup';

export function AppShell({ children }: { children: ReactNode }) {
  return (
    <TooltipProvider>
      <SidebarProvider>
        <Sidebar collapsible="icon" className="border-r border-sidebar-border">
          <SidebarHeader className="px-4 py-4 group-data-[collapsible=icon]:px-2">
            <Link
              href="/"
              className="flex items-center gap-2 text-[15px] font-semibold tracking-tight text-foreground"
            >
              <span
                aria-hidden
                className="grid size-6 shrink-0 grid-cols-2 gap-0.5 rounded-sm p-1"
              >
                <span className="rounded-[1px] bg-muted-foreground/50" />
                <span className="rounded-[1px] bg-muted-foreground/50" />
                <span className="rounded-[1px] bg-sidebar-primary" />
                <span className="rounded-[1px] bg-muted-foreground/50" />
              </span>
              <span className="group-data-[collapsible=icon]:sr-only">
                LeaveWise
              </span>
            </Link>
          </SidebarHeader>
          <SidebarContent>
            <nav aria-label="Main" className="flex flex-col">
              <NavGroup section="main" />
              <AdminNavBoundary>
                <Suspense fallback={null}>
                  <AdminNav />
                </Suspense>
              </AdminNavBoundary>
            </nav>
          </SidebarContent>
        </Sidebar>
        <SidebarInset className="min-w-0 bg-background">
          <header className="flex h-14 items-center gap-3 border-b px-4 md:px-6">
            <SidebarTrigger className="-ml-1" />
            <div className="ml-auto flex items-center gap-3">
              <HeaderUserBoundary>
                <Suspense fallback={<HeaderUserSkeleton />}>
                  <HeaderUser />
                </Suspense>
              </HeaderUserBoundary>
            </div>
          </header>
          <main className="flex-1 px-4 py-6 md:px-8 md:py-8">{children}</main>
        </SidebarInset>
      </SidebarProvider>
    </TooltipProvider>
  );
}

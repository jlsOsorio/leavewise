'use client';

import type { LucideIcon } from 'lucide-react';
import {
  BadgeCheck,
  Building2,
  CalendarDays,
  CalendarRange,
  Clock,
  FileSpreadsheet,
  Flag,
  Landmark,
  Lightbulb,
  LogOut,
  ScrollText,
  UserRound,
  Users,
  Wallet,
} from 'lucide-react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import type { ReactNode } from 'react';

import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarHeader,
  SidebarInset,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarProvider,
  SidebarTrigger,
} from '@/components/ui/sidebar';
import { cn } from '@/lib/utils';

type NavItem = { href: string; label: string; icon: LucideIcon };

const MAIN_NAV: NavItem[] = [
  { href: '/calendar', label: 'Calendar', icon: CalendarDays },
  { href: '/vacations', label: 'Vacations', icon: CalendarRange },
  { href: '/suggestions', label: 'Suggestions', icon: Lightbulb },
  { href: '/imports', label: 'Imports', icon: FileSpreadsheet },
  { href: '/profile', label: 'Profile', icon: UserRound },
];

const ADMIN_NAV: NavItem[] = [
  { href: '/admin/approvals', label: 'Approvals', icon: BadgeCheck },
  { href: '/admin/users', label: 'Users', icon: Users },
  { href: '/admin/entitlements', label: 'Entitlements', icon: Wallet },
  { href: '/admin/companies', label: 'Companies', icon: Building2 },
  { href: '/admin/municipalities', label: 'Municipalities', icon: Landmark },
  { href: '/admin/holidays', label: 'Holidays', icon: Flag },
  { href: '/admin/policies', label: 'Policies', icon: ScrollText },
  { href: '/admin/schedules', label: 'Work schedules', icon: Clock },
];

export type ShellUser = {
  name: string;
  email: string;
  company: string;
  role: 'USER' | 'ADMIN';
};

type AppShellProps = {
  user: ShellUser;
  children: ReactNode;
  /** Renders inline (no fixed positioning) for previews; also forces the active item. */
  preview?: { activeHref: string };
};

function initials(name: string) {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join('');
}

function NavGroup({
  label,
  items,
  activeHref,
}: {
  label?: string;
  items: NavItem[];
  activeHref: string;
}) {
  return (
    <SidebarGroup>
      {label && <SidebarGroupLabel>{label}</SidebarGroupLabel>}
      <SidebarGroupContent>
        <SidebarMenu>
          {items.map(({ href, label: itemLabel, icon: Icon }) => {
            const active =
              activeHref === href || activeHref.startsWith(`${href}/`);
            return (
              <SidebarMenuItem key={href}>
                <SidebarMenuButton
                  asChild
                  isActive={active}
                  className="data-[active=true]:font-medium data-[active=true]:[&_svg]:text-sidebar-primary"
                >
                  <Link href={href} aria-current={active ? 'page' : undefined}>
                    <Icon />
                    <span>{itemLabel}</span>
                  </Link>
                </SidebarMenuButton>
              </SidebarMenuItem>
            );
          })}
        </SidebarMenu>
      </SidebarGroupContent>
    </SidebarGroup>
  );
}

export function AppShell({ user, children, preview }: AppShellProps) {
  const pathname = usePathname();
  const activeHref = preview?.activeHref ?? pathname;

  return (
    <SidebarProvider className={cn(preview && 'min-h-0')}>
      <Sidebar
        collapsible={preview ? 'none' : 'icon'}
        className="border-r border-sidebar-border"
      >
        <SidebarHeader className="px-4 py-4">
          <Link
            href="/calendar"
            className="flex items-center gap-2 text-[15px] font-semibold tracking-tight text-foreground"
          >
            <span
              aria-hidden
              className="grid size-6 grid-cols-2 gap-0.5 rounded-sm p-1"
            >
              <span className="rounded-[1px] bg-muted-foreground/50" />
              <span className="rounded-[1px] bg-muted-foreground/50" />
              <span className="rounded-[1px] bg-sidebar-primary" />
              <span className="rounded-[1px] bg-muted-foreground/50" />
            </span>
            LeaveWise
          </Link>
        </SidebarHeader>
        <SidebarContent>
          <NavGroup items={MAIN_NAV} activeHref={activeHref} />
          {user.role === 'ADMIN' && (
            <NavGroup
              label="Administration"
              items={ADMIN_NAV}
              activeHref={activeHref}
            />
          )}
        </SidebarContent>
        <SidebarFooter className="px-4 py-3 text-xs text-muted-foreground">
          {user.company}
        </SidebarFooter>
      </Sidebar>
      <SidebarInset className="min-w-0 bg-background">
        <header className="flex h-14 items-center gap-3 border-b px-4 md:px-6">
          {!preview && <SidebarTrigger className="-ml-1" />}
          <div className="ml-auto flex items-center gap-3">
            <DropdownMenu>
              <DropdownMenuTrigger className="flex items-center gap-2.5 rounded-md px-1.5 py-1 text-left outline-none hover:bg-muted focus-visible:ring-3 focus-visible:ring-ring/50">
                <span className="hidden text-right leading-tight sm:block">
                  <span className="block text-[13px] font-medium">
                    {user.name}
                  </span>
                  <span className="block text-xs text-muted-foreground">
                    {user.role === 'ADMIN' ? 'Administrator' : 'Employee'}
                  </span>
                </span>
                <Avatar className="size-8">
                  <AvatarFallback className="bg-raised text-xs font-medium">
                    {initials(user.name)}
                  </AvatarFallback>
                </Avatar>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" className="w-56">
                <DropdownMenuLabel className="font-normal">
                  <span className="block text-[13px] font-medium text-foreground">
                    {user.name}
                  </span>
                  <span className="block truncate text-xs text-muted-foreground">
                    {user.email}
                  </span>
                </DropdownMenuLabel>
                <DropdownMenuSeparator />
                <DropdownMenuItem asChild>
                  <Link href="/profile">
                    <UserRound />
                    Profile
                  </Link>
                </DropdownMenuItem>
                <DropdownMenuItem>
                  <LogOut />
                  Log out
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
        </header>
        <div className="flex-1 px-4 py-6 md:px-8 md:py-8">{children}</div>
      </SidebarInset>
    </SidebarProvider>
  );
}

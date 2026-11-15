'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';

import {
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
} from '@/components/ui/sidebar';
import type { NavItem, NavSection } from './nav';
import { NAV } from './nav';

function isActive(pathname: string, href: string) {
  return pathname === href || pathname.startsWith(`${href}/`);
}

// Takes a section key, not items: Lucide icons are functions and can't cross the server/client boundary
export function NavGroup({
  section,
  label,
}: {
  section: NavSection;
  label?: string;
}) {
  const pathname = usePathname();
  const items: NavItem[] = NAV[section];

  if (items.length === 0) {
    return null;
  }

  return (
    <SidebarGroup>
      {label && <SidebarGroupLabel>{label}</SidebarGroupLabel>}
      <SidebarGroupContent>
        <SidebarMenu>
          {items.map(({ href, label: itemLabel, icon: Icon }) => {
            const active = isActive(pathname, href);
            return (
              <SidebarMenuItem key={href}>
                <SidebarMenuButton
                  asChild
                  isActive={active}
                  tooltip={itemLabel}
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

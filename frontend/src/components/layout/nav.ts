import type { LucideIcon } from 'lucide-react';
import { House, MapPin } from 'lucide-react';

export type NavItem = { href: string; label: string; icon: LucideIcon };

export const NAV = {
  main: [{ href: '/', label: 'Home', icon: House }],
  admin: [
    { href: '/admin/municipalities', label: 'Municipalities', icon: MapPin },
  ],
} satisfies Record<string, NavItem[]>;

export type NavSection = keyof typeof NAV;

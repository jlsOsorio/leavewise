import type { LucideIcon } from 'lucide-react';
import { House } from 'lucide-react';

export type NavItem = { href: string; label: string; icon: LucideIcon };

export const NAV = {
  main: [{ href: '/', label: 'Home', icon: House }],
  admin: [],
} satisfies Record<string, NavItem[]>;

export type NavSection = keyof typeof NAV;

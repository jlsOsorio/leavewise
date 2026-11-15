'use client';

import { Loader2, LogOut } from 'lucide-react';
import { useTransition } from 'react';

import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { logout } from '@/lib/auth/actions';

type UserMenuProps = {
  name: string;
  email: string;
  company: string;
};

function initials(name: string) {
  const parts = name.split(/\s+/).filter(Boolean);
  const first = parts[0]?.[0] ?? '';
  const last = parts.length > 1 ? (parts.at(-1)?.[0] ?? '') : '';
  return (first + last).toUpperCase();
}

export function UserMenu({ name, email, company }: UserMenuProps) {
  const [loggingOut, startLogout] = useTransition();

  // Keep the menu open so the pending state stays visible until the redirect
  function handleLogout(event: Event) {
    event.preventDefault();
    startLogout(logout);
  }

  return (
    <DropdownMenu>
      <DropdownMenuTrigger className="flex items-center gap-2.5 rounded-md px-1.5 py-1 text-left outline-none hover:bg-muted focus-visible:ring-3 focus-visible:ring-ring/50">
        <span className="sr-only">User menu:</span>
        <span className="hidden min-w-0 text-right leading-tight sm:block">
          <span className="block max-w-48 truncate text-[13px] font-medium">
            {name}
          </span>
          <span className="block max-w-48 truncate text-xs text-muted-foreground">
            {company}
          </span>
        </span>
        <Avatar className="size-8" aria-hidden>
          <AvatarFallback className="bg-raised text-xs font-medium">
            {initials(name)}
          </AvatarFallback>
        </Avatar>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-56">
        <DropdownMenuLabel className="font-normal">
          <span className="block truncate text-[13px] font-medium text-foreground">
            {name}
          </span>
          <span className="block truncate text-xs text-muted-foreground">
            {email}
          </span>
        </DropdownMenuLabel>
        <DropdownMenuSeparator />
        <DropdownMenuItem disabled={loggingOut} onSelect={handleLogout}>
          {loggingOut ? (
            <Loader2 className="animate-spin" aria-hidden />
          ) : (
            <LogOut aria-hidden />
          )}
          Log out
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}

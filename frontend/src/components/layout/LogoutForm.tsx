'use client';

import { Loader2, LogOut } from 'lucide-react';
import { useFormStatus } from 'react-dom';

import { Button } from '@/components/ui/button';
import { logout } from '@/lib/auth/actions';

function LogoutButton() {
  const { pending } = useFormStatus();

  return (
    <Button type="submit" variant="ghost" size="sm" disabled={pending}>
      {pending ? (
        <Loader2 className="animate-spin" aria-hidden />
      ) : (
        <LogOut aria-hidden />
      )}
      <span className="sr-only sm:not-sr-only">Log out</span>
    </Button>
  );
}

export function LogoutForm() {
  return (
    <form action={logout}>
      <LogoutButton />
    </form>
  );
}

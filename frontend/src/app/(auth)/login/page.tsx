import type { Metadata } from 'next';
import Link from 'next/link';

import { errorMessage } from '@/lib/api/errors';
import { LoginForm } from './_components/LoginForm';

export const metadata: Metadata = { title: 'Sign in' };

export default async function LoginPage({ searchParams }: PageProps<'/login'>) {
  const { next, session } = await searchParams;

  return (
    <div className="space-y-6">
      <div className="space-y-1">
        <h1 className="text-[25px] leading-tight">Sign in</h1>
        <p className="text-muted-foreground">
          Use the email you registered with.
        </p>
      </div>
      {session === 'expired' && (
        <p role="status" className="text-warning">
          {errorMessage('UNAUTHENTICATED')}
        </p>
      )}
      <LoginForm next={typeof next === 'string' ? next : undefined} />
      <p className="text-muted-foreground">
        New to LeaveWise?{' '}
        <Link
          href="/register"
          className="font-medium text-foreground underline-offset-4 hover:underline"
        >
          Create an account
        </Link>
      </p>
    </div>
  );
}

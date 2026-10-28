import type { Metadata } from 'next';
import Link from 'next/link';

import { RegisterForm } from './_components/RegisterForm';

export const metadata: Metadata = { title: 'Create account' };

export default function RegisterPage() {
  return (
    <div className="space-y-6">
      <div className="space-y-1">
        <h1 className="text-[25px] leading-tight">Create an account</h1>
        <p className="text-muted-foreground">
          Pick the company and location you work at.
        </p>
        <p className="text-xs text-muted-foreground">
          Fields marked <span className="text-destructive">*</span> are
          required.
        </p>
      </div>
      <RegisterForm />
      <p className="text-muted-foreground">
        Already have an account?{' '}
        <Link
          href="/login"
          className="font-medium text-foreground underline-offset-4 hover:underline"
        >
          Sign in
        </Link>
      </p>
    </div>
  );
}

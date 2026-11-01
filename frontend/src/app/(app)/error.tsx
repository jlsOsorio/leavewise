'use client';

import type { ErrorInfo } from 'next/error';

import { Button } from '@/components/ui/button';
import { errorMessage } from '@/lib/api/errors';

export default function Error({ retry }: ErrorInfo) {
  function handleRetry() {
    retry();
  }

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-semibold tracking-tight">
        This page couldn&apos;t be loaded
      </h1>
      <p className="text-muted-foreground">{errorMessage('INTERNAL_ERROR')}</p>
      <Button variant="outline" onClick={handleRetry}>
        Try again
      </Button>
    </div>
  );
}

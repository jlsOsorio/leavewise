'use client';

import { RotateCw } from 'lucide-react';
import { catchError, type ErrorInfo } from 'next/error';

import { Button } from '@/components/ui/button';

function HeaderUserFallback(_props: object, { retry }: ErrorInfo) {
  function handleRetry() {
    retry();
  }

  return (
    <p className="flex items-center gap-2 text-xs text-muted-foreground">
      Couldn&apos;t load your details.
      <Button variant="ghost" size="xs" onClick={handleRetry}>
        <RotateCw aria-hidden />
        Retry
      </Button>
    </p>
  );
}

export const HeaderUserBoundary = catchError(HeaderUserFallback);

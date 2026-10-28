export function safeNext(next: string | undefined): string {
  if (
    !next?.startsWith('/') ||
    next.startsWith('//') ||
    next.startsWith('/\\')
  ) {
    return '/';
  }
  return next;
}

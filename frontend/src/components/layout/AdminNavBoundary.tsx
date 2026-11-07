'use client';

import { catchError } from 'next/error';

// The user menu already reports a failed /me; the admin group just stays hidden
export const AdminNavBoundary = catchError(() => null);

import type { RequestHandler } from 'msw';

// Default happy-path mocks of the backend (`BACKEND_URL`) and of our own Route Handlers.
// Override per test with `server.use(...)`.
export const handlers: RequestHandler[] = [];

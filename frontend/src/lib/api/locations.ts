import type { Municipality } from './municipalities';

export type LocationDetails = {
  id: number;
  companyId: number;
  name: string;
  address: string | null;
  municipality: Municipality;
};

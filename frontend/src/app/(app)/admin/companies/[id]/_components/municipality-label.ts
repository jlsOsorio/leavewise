import type { Municipality } from '@/lib/api/municipalities';

export function municipalityLabel(municipality: Municipality): string {
  return `${municipality.name} (${municipality.district})`;
}

import { getMe } from '@/lib/api/me';
import { NavGroup } from './NavGroup';

export async function AdminNav() {
  const me = await getMe();

  if (me.role !== 'ADMIN') {
    return null;
  }
  return <NavGroup section="admin" label="Administration" />;
}

import { getMe } from '@/lib/api/me';
import { UserMenu } from './UserMenu';

export async function HeaderUser() {
  const me = await getMe();

  return <UserMenu name={me.name} email={me.email} company={me.company.name} />;
}

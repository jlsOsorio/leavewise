import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import { getMe } from '@/lib/api/me';

function initials(name: string) {
  const parts = name.split(/\s+/).filter(Boolean);
  const first = parts[0]?.[0] ?? '';
  const last = parts.length > 1 ? (parts.at(-1)?.[0] ?? '') : '';
  return (first + last).toUpperCase();
}

export async function HeaderUser() {
  const me = await getMe();

  return (
    <div className="flex items-center gap-2.5">
      <span className="min-w-0 text-right leading-tight">
        <span className="block max-w-48 truncate text-[13px] font-medium">
          {me.name}
        </span>
        <span className="block max-w-48 truncate text-xs text-muted-foreground">
          {me.company.name}
        </span>
      </span>
      <Avatar className="size-8" aria-hidden>
        <AvatarFallback className="bg-raised text-xs font-medium">
          {initials(me.name)}
        </AvatarFallback>
      </Avatar>
    </div>
  );
}

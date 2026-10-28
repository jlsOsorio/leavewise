import { cn } from '@/lib/utils';

export type DayKind = 'workday' | 'weekend' | 'holiday';

type YearStripProps = {
  year: number;
  holidays: string[];
  booked: string[];
  blocks: { start: string; end: string }[];
  label: string;
};

const MONTHS = [
  'Jan',
  'Feb',
  'Mar',
  'Apr',
  'May',
  'Jun',
  'Jul',
  'Aug',
  'Sep',
  'Oct',
  'Nov',
  'Dec',
];

function iso(year: number, month: number, day: number) {
  return `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
}

/** Wall-planner view of a year: one row per month, one cell per day of the month. */
export function YearStrip({
  year,
  holidays,
  booked,
  blocks,
  label,
}: YearStripProps) {
  const holidaySet = new Set(holidays);
  const bookedSet = new Set(booked);
  const inBlock = (date: string) =>
    blocks.some(({ start, end }) => date >= start && date <= end);

  return (
    <div role="img" aria-label={label} className="w-full">
      <div className="grid grid-cols-[2.25rem_1fr] gap-y-0.75">
        {MONTHS.map((month, m) => {
          const days = new Date(Date.UTC(year, m + 1, 0)).getUTCDate();
          return (
            <div key={month} className="contents">
              <span className="self-center text-[11px] leading-none text-muted-foreground">
                {month}
              </span>
              <div className="grid grid-cols-31 gap-0.75">
                {Array.from({ length: days }, (_, d) => {
                  const date = iso(year, m, d + 1);
                  const weekday = new Date(
                    Date.UTC(year, m, d + 1),
                  ).getUTCDay();
                  const kind: DayKind = holidaySet.has(date)
                    ? 'holiday'
                    : weekday === 0 || weekday === 6
                      ? 'weekend'
                      : 'workday';
                  const isBooked = bookedSet.has(date);
                  const block = inBlock(date);
                  return (
                    <span
                      key={date}
                      className={cn(
                        'aspect-square rounded-[2px]',
                        kind === 'workday' && 'bg-foreground/[0.07]',
                        kind === 'weekend' && 'bg-foreground/18',
                        kind === 'holiday' && 'bg-holiday',
                        block && !isBooked && 'ring-1 ring-primary ring-inset',
                        isBooked && 'bg-primary',
                      )}
                    />
                  );
                })}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

export function YearStripLegend() {
  const items = [
    { label: 'Weekend', className: 'bg-foreground/18' },
    { label: 'Public holiday', className: 'bg-holiday' },
    { label: 'Vacation day', className: 'bg-primary' },
    {
      label: 'Day off in the same break',
      className: 'ring-1 ring-primary ring-inset',
    },
  ];
  return (
    <ul className="flex flex-wrap gap-x-5 gap-y-2 text-xs text-muted-foreground">
      {items.map((item) => (
        <li key={item.label} className="flex items-center gap-2">
          <span
            aria-hidden
            className={cn('size-2.5 rounded-[2px]', item.className)}
          />
          {item.label}
        </li>
      ))}
    </ul>
  );
}

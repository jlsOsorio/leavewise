import type { ReactNode } from 'react';

import { YearStrip, YearStripLegend } from '@/components/layout/YearStrip';

// Portuguese national holidays 2027 (fixed dates + Easter-based: Good Friday, Easter, Corpus Christi)
const HOLIDAYS_2027 = [
  '2027-01-01',
  '2027-03-26',
  '2027-03-28',
  '2027-04-25',
  '2027-05-01',
  '2027-05-27',
  '2027-06-10',
  '2027-08-15',
  '2027-10-05',
  '2027-11-01',
  '2027-12-01',
  '2027-12-08',
  '2027-12-25',
];

const BOOKED = ['2027-06-07', '2027-06-08', '2027-06-09', '2027-06-11'];
const BLOCKS = [{ start: '2027-06-05', end: '2027-06-13' }];

/**
 * Split-screen frame for /login and /register. The brand side shows the product's idea in one picture:
 * a year of Portuguese holidays and the break four vacation days buy around Portugal Day.
 */
export function AuthLayout({ children }: { children: ReactNode }) {
  return (
    <div className="grid min-h-svh lg:grid-cols-[minmax(0,1.15fr)_minmax(0,1fr)]">
      <aside className="relative hidden flex-col justify-between gap-12 border-r bg-sidebar px-12 py-10 lg:flex xl:px-16">
        <span className="text-[15px] font-semibold tracking-tight">
          LeaveWise
        </span>
        <div className="max-w-xl space-y-8">
          <h1 className="text-display leading-[1.05]">
            Four days booked.
            <br />
            <span className="text-muted-foreground">Nine days away.</span>
          </h1>
          <p className="max-w-md text-[15px] leading-relaxed text-muted-foreground">
            Book 7–9 and 11 June around Portugal Day and you&apos;re off from
            Saturday the 5th to Sunday the 13th. LeaveWise finds these breaks
            for your location and keeps your balance up to date.
          </p>
          <div className="space-y-4">
            <YearStrip
              year={2027}
              holidays={HOLIDAYS_2027}
              booked={BOOKED}
              blocks={BLOCKS}
              label="2027 planner: weekends and Portuguese public holidays, with vacation booked on 7, 8, 9 and 11 June giving nine consecutive days off from 5 to 13 June."
            />
            <YearStripLegend />
          </div>
        </div>
        <p className="text-xs text-muted-foreground">
          Vacation planning for employees of Portuguese companies.
        </p>
      </aside>
      <main className="flex items-center justify-center px-4 py-12 sm:px-8">
        <div className="w-full max-w-sm">{children}</div>
      </main>
    </div>
  );
}

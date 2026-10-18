// TEMPORARY style tile for reviewing the design system. Delete once real pages exist.
import { Check, Download, Loader2, Plus, X } from 'lucide-react';
import type { Metadata } from 'next';
import {
  IBM_Plex_Mono,
  IBM_Plex_Sans,
  Inter_Tight,
  JetBrains_Mono,
} from 'next/font/google';
import Link from 'next/link';
import type { ReactNode } from 'react';

import { DesignControls } from './_components/DesignControls';
import type { TypePair } from './_components/DesignControls';
import { UploadDemo } from './_components/UploadDemo';
import { AppShell } from '@/components/layout/AppShell';
import { AuthLayout } from '@/components/layout/AuthLayout';
import { DataTable } from '@/components/layout/DataTable';
import type { Column } from '@/components/layout/DataTable';
import { PageHeader } from '@/components/layout/PageHeader';
import {
  Stat,
  StatGroup,
  StatGroupSkeleton,
} from '@/components/layout/StatGroup';
import { TableSkeleton } from '@/components/layout/TableSkeleton';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';

import './design.css';

export const metadata: Metadata = { title: 'Style tile · LeaveWise' };

const interTight = Inter_Tight({
  variable: '--font-inter-tight',
  subsets: ['latin'],
});
const jetbrains = JetBrains_Mono({
  variable: '--font-jetbrains',
  subsets: ['latin'],
});
const plexSans = IBM_Plex_Sans({
  variable: '--font-plex-sans',
  subsets: ['latin'],
  weight: ['400', '500', '600', '700'],
});
const plexMono = IBM_Plex_Mono({
  variable: '--font-plex-mono',
  subsets: ['latin'],
  weight: ['400', '500'],
});

const TYPE_PAIRS: TypePair[] = [
  { id: 'geist', label: 'Geist' },
  {
    id: 'inter',
    label: 'Inter Tight',
    sansVar: '--font-inter-tight',
    monoVar: '--font-jetbrains',
  },
  {
    id: 'plex',
    label: 'IBM Plex',
    sansVar: '--font-plex-sans',
    monoVar: '--font-plex-mono',
  },
];

type Status = 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'CANCELLED';

type VacationRow = {
  id: number;
  employee: string;
  nif: string;
  start: string;
  end: string;
  days: number;
  status: Status;
};

const VACATIONS: VacationRow[] = [
  {
    id: 412,
    employee: 'Inês Carvalho',
    nif: '245 871 093',
    start: '07/06/2027',
    end: '11/06/2027',
    days: 4,
    status: 'PENDING',
  },
  {
    id: 409,
    employee: 'Rui Fernandes',
    nif: '213 554 870',
    start: '02/08/2027',
    end: '20/08/2027',
    days: 14,
    status: 'ACCEPTED',
  },
  {
    id: 405,
    employee: 'Marta Sousa',
    nif: '198 330 214',
    start: '04/10/2027',
    end: '04/10/2027',
    days: 1,
    status: 'PENDING',
  },
  {
    id: 398,
    employee: 'Tiago Almeida',
    nif: '256 102 447',
    start: '27/12/2027',
    end: '31/12/2027',
    days: 5,
    status: 'REJECTED',
  },
  {
    id: 391,
    employee: 'Beatriz Nunes',
    nif: '231 908 665',
    start: '19/04/2027',
    end: '23/04/2027',
    days: 5,
    status: 'CANCELLED',
  },
];

const STATUS_LABEL: Record<Status, string> = {
  PENDING: 'Pending',
  ACCEPTED: 'Accepted',
  REJECTED: 'Rejected',
  CANCELLED: 'Cancelled',
};

function StatusBadge({ status }: { status: Status }) {
  if (status === 'PENDING') return <Badge variant="warning">Pending</Badge>;
  if (status === 'ACCEPTED') return <Badge variant="success">Accepted</Badge>;
  return (
    <Badge variant="outline" className="text-muted-foreground line-through">
      {STATUS_LABEL[status]}
    </Badge>
  );
}

const VACATION_COLUMNS: Column<VacationRow>[] = [
  {
    key: 'employee',
    header: 'Employee',
    cell: (row) => (
      <span className="flex flex-col">
        <span className="font-medium">{row.employee}</span>
        <span className="font-mono text-xs text-muted-foreground">
          NIF {row.nif}
        </span>
      </span>
    ),
  },
  {
    key: 'dates',
    header: 'Dates',
    cell: (row) => (
      <span className="num">
        {row.start === row.end ? row.start : `${row.start} – ${row.end}`}
      </span>
    ),
  },
  {
    key: 'days',
    header: 'Working days',
    align: 'right',
    cell: (row) => row.days,
  },
  {
    key: 'status',
    header: 'Status',
    cell: (row) => <StatusBadge status={row.status} />,
  },
];

const APPROVAL_COLUMNS: Column<VacationRow>[] = [
  ...VACATION_COLUMNS.filter((col) => col.key !== 'status'),
  {
    key: 'actions',
    header: 'Decision',
    align: 'right',
    cell: () => (
      <span className="inline-flex gap-1.5">
        <Button size="sm" variant="outline">
          <X />
          Reject
        </Button>
        <Button size="sm">
          <Check />
          Approve
        </Button>
      </span>
    ),
  },
];

function Section({
  title,
  description,
  children,
}: {
  title: string;
  description: string;
  children: ReactNode;
}) {
  return (
    <section className="space-y-5 border-t pt-10">
      <div className="max-w-2xl space-y-1">
        <h2 className="text-xl">{title}</h2>
        <p className="text-muted-foreground">{description}</p>
      </div>
      {children}
    </section>
  );
}

const SURFACES = [
  { name: 'Sidebar', token: 'bg-sidebar', role: 'Deepest layer, navigation' },
  { name: 'Background', token: 'bg-background', role: 'Page' },
  { name: 'Card', token: 'bg-card', role: 'Panels, tables' },
  { name: 'Raised', token: 'bg-raised', role: 'Popovers, icon wells' },
  { name: 'Muted', token: 'bg-muted', role: 'Hover, skeletons' },
];

const SIGNALS = [
  {
    name: 'Accent',
    token: 'bg-primary',
    role: 'Primary actions, focus, booked days',
  },
  { name: 'Success', token: 'bg-success', role: 'Accepted' },
  { name: 'Warning', token: 'bg-warning', role: 'Pending' },
  { name: 'Destructive', token: 'bg-destructive', role: 'Errors, delete' },
  {
    name: 'Holiday',
    token: 'bg-holiday',
    role: 'Public holidays on the calendar',
  },
];

const TYPE_SCALE = [
  { size: 'text-display', px: '49', sample: 'Nine days away' },
  { size: 'text-[39px]', px: '39', sample: 'Calendar 2027' },
  { size: 'text-[31px]', px: '31', sample: '13 days available' },
  { size: 'text-[25px]', px: '25', sample: 'Approvals' },
  { size: 'text-xl', px: '20', sample: 'Balance for 2027' },
  { size: 'text-base', px: '16', sample: 'Request vacation for 7–11 June' },
  {
    size: 'text-sm',
    px: '14',
    sample:
      'Your manager reviews pending requests. You can cancel an accepted vacation until its first day.',
  },
  { size: 'text-xs', px: '12', sample: 'Last updated 16/10/2026 at 14:32' },
];

export default function DesignPage() {
  const fontClassName = [
    interTight.variable,
    jetbrains.variable,
    plexSans.variable,
    plexMono.variable,
  ].join(' ');

  return (
    <DesignControls typePairs={TYPE_PAIRS} fontClassName={fontClassName}>
      <div className="mx-auto max-w-6xl space-y-14 px-4 py-10 md:px-8">
        <header className="max-w-2xl space-y-2">
          <h1 className="text-[31px] leading-tight">LeaveWise design system</h1>
          <p className="text-muted-foreground">
            Dark only. A graphite base where depth comes from lightness, not
            shadows, and one accent kept for actions and booked days. Switch the
            accent and typeface above to compare.
          </p>
        </header>

        <Section
          title="Hero"
          description="The sign-in and registration frame. A 2027 planner shows the core idea in one picture: a few vacation days around a holiday become a long break."
        >
          <div className="overflow-hidden rounded-xl border">
            <AuthLayout>
              <div className="space-y-6">
                <div className="space-y-1">
                  <h2 className="text-[25px] leading-tight">Sign in</h2>
                  <p className="text-muted-foreground">
                    Use the email you registered with.
                  </p>
                </div>
                <form className="space-y-4">
                  <div className="space-y-1.5">
                    <Label htmlFor="d-email">Email</Label>
                    <Input
                      id="d-email"
                      type="email"
                      autoComplete="email"
                      placeholder="ines.carvalho@empresa.pt"
                    />
                  </div>
                  <div className="space-y-1.5">
                    <Label htmlFor="d-password">Password</Label>
                    <Input
                      id="d-password"
                      type="password"
                      autoComplete="current-password"
                    />
                  </div>
                  <Button type="button" className="h-9 w-full">
                    Sign in
                  </Button>
                </form>
                <p className="text-muted-foreground">
                  New to LeaveWise?{' '}
                  <Link
                    href="/register"
                    className="font-medium text-foreground underline-offset-4 hover:underline"
                  >
                    Create an account
                  </Link>
                </p>
              </div>
            </AuthLayout>
          </div>
        </Section>

        <Section
          title="Colour"
          description="Surfaces step up in lightness from the sidebar to popovers. Status colours only ever label a vacation's state."
        >
          <div className="grid gap-6 md:grid-cols-2">
            {[SURFACES, SIGNALS].map((group, i) => (
              <ul key={i} className="overflow-hidden rounded-lg border">
                {group.map((swatch) => (
                  <li
                    key={swatch.name}
                    className="flex items-center gap-4 border-b px-4 py-3 last:border-b-0"
                  >
                    <span
                      className={`size-9 shrink-0 rounded-md border ${swatch.token}`}
                    />
                    <span className="min-w-0 flex-1">
                      <span className="block text-[13px] font-medium">
                        {swatch.name}
                      </span>
                      <span className="block text-xs text-muted-foreground">
                        {swatch.role}
                      </span>
                    </span>
                  </li>
                ))}
              </ul>
            ))}
          </div>
          <div className="flex flex-wrap gap-x-8 gap-y-2">
            <span>Primary text on background</span>
            <span className="text-muted-foreground">Secondary text</span>
            <span className="text-primary">Accent text</span>
          </div>
        </Section>

        <Section
          title="Typography"
          description="One family, weight and size do the work. Headings are semibold with tight tracking; numbers are tabular so columns line up."
        >
          <ul className="divide-y rounded-lg border bg-card">
            {TYPE_SCALE.map((step) => (
              <li
                key={step.px}
                className="grid grid-cols-[3rem_1fr] items-baseline gap-4 px-5 py-4"
              >
                <span className="text-xs num text-muted-foreground">
                  {step.px}
                </span>
                <span
                  className={`${step.size} ${Number(step.px) >= 20 ? 'leading-tight font-semibold tracking-tight' : 'max-w-prose'}`}
                >
                  {step.sample}
                </span>
              </li>
            ))}
            <li className="grid grid-cols-[3rem_1fr] items-baseline gap-4 px-5 py-4">
              <span className="text-xs text-muted-foreground">Mono</span>
              <span className="font-mono text-[13px]">
                NIF 245 871 093 · Import #1042
              </span>
            </li>
          </ul>
        </Section>

        <Section
          title="Controls"
          description="Primary actions take the accent. A pending action keeps its label, disables itself and shows a spinner."
        >
          <div className="flex flex-wrap items-center gap-2">
            <Button>
              <Plus />
              Request vacation
            </Button>
            <Button variant="outline">
              <Download />
              Download template
            </Button>
            <Button variant="secondary">Generate for 2027</Button>
            <Button variant="outline">Cancel</Button>
            <Button variant="destructive">Delete vacation</Button>
            <Button disabled>
              <Loader2 className="animate-spin" />
              Requesting vacation
            </Button>
          </div>
          <div className="grid max-w-2xl gap-5 sm:grid-cols-2">
            <div className="space-y-1.5">
              <Label htmlFor="d-name">Full name</Label>
              <Input id="d-name" defaultValue="Inês Carvalho" />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="d-nif">NIF</Label>
              <Input
                id="d-nif"
                defaultValue="245871094"
                aria-invalid
                aria-describedby="d-nif-error"
                className="font-mono"
              />
              <p id="d-nif-error" className="text-xs text-destructive">
                This NIF&apos;s check digit doesn&apos;t match. Check the number
                on your card.
              </p>
            </div>
          </div>
          <div className="flex flex-wrap gap-2">
            {(['PENDING', 'ACCEPTED', 'REJECTED', 'CANCELLED'] as const).map(
              (status) => (
                <StatusBadge key={status} status={status} />
              ),
            )}
          </div>
        </Section>

        <Section
          title="Dashboard stats"
          description="The balance reads as one sum, so it sits in one panel. Only the number you act on, available days, takes the accent."
        >
          <StatGroup
            footer={
              <div className="space-y-2">
                <div
                  className="flex h-2 overflow-hidden rounded-full bg-muted"
                  role="img"
                  aria-label="8 accepted, 4 pending, 13 available of 25 days"
                >
                  <span className="bg-primary" style={{ width: '32%' }} />
                  <span className="bg-warning" style={{ width: '16%' }} />
                </div>
                <p className="text-xs text-muted-foreground">
                  12 of 25 days used or requested
                </p>
              </div>
            }
          >
            <Stat
              label="Annual"
              value={22}
              unit="days"
              hint="From your entitlement"
            />
            <Stat label="Carried over" value={3} unit="days" hint="From 2026" />
            <Stat label="Accepted" value={8} unit="days" />
            <Stat label="Pending" value={4} unit="days" hint="2 requests" />
            <Stat label="Available" value={13} unit="days" emphasis />
          </StatGroup>
          <StatGroupSkeleton />
        </Section>

        <Section
          title="Tables"
          description="Dense rows, hairline dividers, right-aligned numbers. The loading state keeps the table's shape so nothing jumps."
        >
          <DataTable
            caption="Vacations"
            columns={VACATION_COLUMNS}
            rows={VACATIONS}
            rowKey={(row) => row.id}
            pageInfo={{ page: 0, size: 5, totalElements: 23, totalPages: 5 }}
            pageHref={(page) => `/design?page=${page}`}
            empty={null}
          />
          <TableSkeleton columns={4} rows={5} />
          <DataTable
            caption="Vacations, empty"
            columns={VACATION_COLUMNS}
            rows={[]}
            rowKey={(row) => row.id}
            pageInfo={{ page: 0, size: 20, totalElements: 0, totalPages: 0 }}
            pageHref={(page) => `/design?page=${page}`}
            empty={
              <div className="mx-auto flex max-w-sm flex-col items-center gap-3 text-center">
                <p className="font-medium">No vacations in 2027 yet</p>
                <p className="text-muted-foreground">
                  Pick days on the calendar or import a spreadsheet.
                </p>
                <Button size="sm">
                  <Plus />
                  Request vacation
                </Button>
              </div>
            }
          />
        </Section>

        <Section
          title="File upload"
          description="Drop or choose an .xlsx up to 5 MB. Wrong type or size is explained right under the area; uploads show progress in place."
        >
          <UploadDemo />
        </Section>

        <Section
          title="Backoffice"
          description="Sidebar navigation with an Administration group for admins, user menu in the header, and pages built from a page header plus tables."
        >
          <div className="overflow-hidden rounded-xl border">
            <AppShell
              preview={{ activeHref: '/admin/approvals' }}
              user={{
                name: 'Joana Ribeiro',
                email: 'joana.ribeiro@empresa.pt',
                company: 'Empresa Exemplo, Lda.',
                role: 'ADMIN',
              }}
            >
              <PageHeader
                title="Approvals"
                description="Pending requests from your company. Approving a request deducts its working days from the employee's balance."
              />
              <DataTable
                caption="Pending approvals"
                columns={APPROVAL_COLUMNS}
                rows={VACATIONS.filter((v) => v.status === 'PENDING')}
                rowKey={(row) => row.id}
                pageInfo={{
                  page: 0,
                  size: 20,
                  totalElements: 2,
                  totalPages: 1,
                }}
                pageHref={(page) => `/design?page=${page}`}
                empty={null}
              />
            </AppShell>
          </div>
        </Section>
      </div>
    </DesignControls>
  );
}

'use client';

import { useState } from 'react';
import type { CSSProperties, ReactNode } from 'react';

import { cn } from '@/lib/utils';

export type TypePair = {
  id: string;
  label: string;
  /** CSS variable names set by next/font; empty means "inherit the root fonts". */
  sansVar?: string;
  monoVar?: string;
};

const ACCENTS = [
  { id: 'emerald', label: 'Emerald', swatch: 'oklch(0.75 0.14 162)' },
  { id: 'blue', label: 'Electric blue', swatch: 'oklch(0.7 0.16 255)' },
  { id: 'amber', label: 'Warm amber', swatch: 'oklch(0.8 0.14 75)' },
  { id: 'violet', label: 'Violet', swatch: 'oklch(0.7 0.16 295)' },
];

function Segmented<T extends { id: string; label: string }>({
  legend,
  name,
  options,
  value,
  onChange,
  renderMark,
}: {
  legend: string;
  name: string;
  options: T[];
  value: string;
  onChange: (id: string) => void;
  renderMark?: (option: T) => ReactNode;
}) {
  return (
    <fieldset className="flex flex-wrap items-center gap-2">
      <legend className="sr-only">{legend}</legend>
      <span aria-hidden className="mr-1 text-xs text-muted-foreground">
        {legend}
      </span>
      <div className="flex flex-wrap rounded-md border bg-card p-0.5">
        {options.map((option) => (
          <label
            key={option.id}
            className={cn(
              'flex cursor-pointer items-center gap-1.5 rounded-sm px-2.5 py-1 text-xs text-muted-foreground transition-colors',
              'has-checked:bg-raised has-checked:text-foreground has-focus-visible:ring-2 has-focus-visible:ring-ring',
            )}
          >
            <input
              type="radio"
              name={name}
              value={option.id}
              checked={value === option.id}
              onChange={() => onChange(option.id)}
              className="sr-only"
            />
            {renderMark?.(option)}
            {option.label}
          </label>
        ))}
      </div>
    </fieldset>
  );
}

export function DesignControls({
  typePairs,
  fontClassName,
  children,
}: {
  typePairs: TypePair[];
  fontClassName: string;
  children: ReactNode;
}) {
  const [accent, setAccent] = useState('emerald');
  const [typeId, setTypeId] = useState(typePairs[0]?.id ?? '');
  const pair = typePairs.find((p) => p.id === typeId);

  const style = {
    ...(pair?.sansVar && { '--font-sans': `var(${pair.sansVar})` }),
    ...(pair?.monoVar && { '--font-geist-mono': `var(${pair.monoVar})` }),
  } as CSSProperties;

  return (
    <div
      data-accent={accent}
      style={style}
      className={cn(fontClassName, 'font-sans')}
    >
      <div className="sticky top-0 z-20 border-b bg-background/90 backdrop-blur">
        <div className="mx-auto flex max-w-6xl flex-wrap items-center gap-x-6 gap-y-3 px-4 py-3 md:px-8">
          <span className="text-[13px] font-semibold tracking-tight">
            LeaveWise style tile
          </span>
          <Segmented
            legend="Accent"
            name="accent"
            options={ACCENTS}
            value={accent}
            onChange={setAccent}
            renderMark={(option) => (
              <span
                aria-hidden
                className="size-2.5 rounded-full"
                style={{ background: option.swatch }}
              />
            )}
          />
          <Segmented
            legend="Typeface"
            name="typeface"
            options={typePairs}
            value={typeId}
            onChange={setTypeId}
          />
        </div>
      </div>
      {children}
    </div>
  );
}

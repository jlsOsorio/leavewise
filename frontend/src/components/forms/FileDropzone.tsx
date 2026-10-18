'use client';

import { FileSpreadsheet, UploadCloud, X } from 'lucide-react';
import { useId, useState } from 'react';
import type { DragEvent } from 'react';

import { Button } from '@/components/ui/button';
import { Progress } from '@/components/ui/progress';
import { cn } from '@/lib/utils';

type Upload = { name: string; size: number; progress: number };

type FileDropzoneProps = {
  /** Comma-separated extensions, e.g. ".xlsx". */
  accept: string;
  maxBytes: number;
  hint: string;
  onFile?: (file: File) => void;
  /** Shown instead of the drop area while a file is uploading. */
  upload?: Upload;
  onCancel?: () => void;
  /** Error coming from the server (e.g. INVALID_FILE), shown under the area. */
  error?: string;
  disabled?: boolean;
};

export function formatBytes(bytes: number) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function validate(file: File, accept: string, maxBytes: number) {
  const extensions = accept.split(',').map((ext) => ext.trim().toLowerCase());
  if (!extensions.some((ext) => file.name.toLowerCase().endsWith(ext))) {
    return `${file.name} isn't a ${extensions.join(' or ')} file. Download the template and fill it in.`;
  }
  if (file.size > maxBytes) {
    return `${file.name} is ${formatBytes(file.size)}. The limit is ${formatBytes(maxBytes)}; split it into smaller files.`;
  }
  return null;
}

export function FileDropzone({
  accept,
  maxBytes,
  hint,
  onFile,
  upload,
  onCancel,
  error,
  disabled,
}: FileDropzoneProps) {
  const id = useId();
  const [dragging, setDragging] = useState(false);
  const [localError, setLocalError] = useState<string | null>(null);
  const shownError = localError ?? error;

  function handleFile(file: File | undefined) {
    if (!file) return;
    const problem = validate(file, accept, maxBytes);
    setLocalError(problem);
    if (!problem) onFile?.(file);
  }

  function onDrop(event: DragEvent<HTMLLabelElement>) {
    event.preventDefault();
    setDragging(false);
    if (!disabled) handleFile(event.dataTransfer.files[0]);
  }

  if (upload) {
    return (
      <div className="flex items-center gap-4 rounded-lg border bg-card px-4 py-4">
        <span className="grid size-10 shrink-0 place-items-center rounded-md bg-raised">
          <FileSpreadsheet className="size-5 text-primary" aria-hidden />
        </span>
        <div className="min-w-0 flex-1 space-y-2">
          <div className="flex items-baseline justify-between gap-3">
            <span className="truncate text-[13px] font-medium">
              {upload.name}
            </span>
            <span className="shrink-0 text-xs num text-muted-foreground">
              {formatBytes(upload.size)} · {upload.progress}%
            </span>
          </div>
          <Progress
            value={upload.progress}
            aria-label={`Uploading ${upload.name}`}
            className="h-1"
          />
        </div>
        {onCancel && (
          <Button
            variant="ghost"
            size="icon-sm"
            onClick={onCancel}
            aria-label="Cancel upload"
          >
            <X />
          </Button>
        )}
      </div>
    );
  }

  return (
    <div className="space-y-2">
      <label
        htmlFor={id}
        onDragOver={(event) => {
          event.preventDefault();
          if (!disabled) setDragging(true);
        }}
        onDragLeave={() => setDragging(false)}
        onDrop={onDrop}
        data-dragging={dragging || undefined}
        data-invalid={shownError ? true : undefined}
        className={cn(
          'group flex cursor-pointer flex-col items-center justify-center gap-3 rounded-lg border border-dashed border-input bg-card px-6 py-10 text-center transition-colors',
          'hover:border-foreground/25 hover:bg-raised/60',
          'focus-within:border-ring focus-within:ring-3 focus-within:ring-ring/40',
          'data-dragging:border-primary data-dragging:bg-primary/[0.06]',
          'data-invalid:border-destructive/60',
          disabled && 'pointer-events-none opacity-50',
        )}
      >
        <span className="grid size-10 place-items-center rounded-md bg-raised transition-colors group-data-dragging:bg-primary group-data-dragging:text-primary-foreground">
          <UploadCloud className="size-5" aria-hidden />
        </span>
        <span className="space-y-1">
          <span className="block text-[13px] font-medium">
            {dragging ? 'Drop to upload' : 'Drag a file here or choose one'}
          </span>
          <span
            id={`${id}-hint`}
            className="block text-xs text-muted-foreground"
          >
            {hint}
          </span>
        </span>
        <input
          id={id}
          type="file"
          accept={accept}
          disabled={disabled}
          aria-describedby={`${id}-hint ${id}-error`}
          className="sr-only"
          onChange={(event) => {
            handleFile(event.target.files?.[0]);
            event.target.value = '';
          }}
        />
      </label>
      <p
        id={`${id}-error`}
        aria-live="polite"
        className="min-h-4 text-xs text-destructive"
      >
        {shownError}
      </p>
    </div>
  );
}

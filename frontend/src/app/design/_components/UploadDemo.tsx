'use client';

import { useEffect, useState } from 'react';

import { FileDropzone } from '@/components/forms/FileDropzone';

const MAX_BYTES = 5 * 1024 * 1024;
const HINT = 'Excel .xlsx, up to 5 MB. One row per vacation.';

export function UploadDemo() {
  const [upload, setUpload] = useState<{
    name: string;
    size: number;
    progress: number;
  }>();

  useEffect(() => {
    if (!upload || upload.progress >= 100) return;
    const timer = setTimeout(
      () =>
        setUpload(
          (u) => u && { ...u, progress: Math.min(100, u.progress + 9) },
        ),
      180,
    );
    return () => clearTimeout(timer);
  }, [upload]);

  return (
    <div className="grid gap-6 lg:grid-cols-3">
      <div className="space-y-2">
        <p className="text-xs text-muted-foreground">Try it: drop any file</p>
        <FileDropzone
          accept=".xlsx"
          maxBytes={MAX_BYTES}
          hint={HINT}
          upload={upload}
          onCancel={() => setUpload(undefined)}
          onFile={(file) =>
            setUpload({ name: file.name, size: file.size, progress: 0 })
          }
        />
      </div>
      <div className="space-y-2">
        <p className="text-xs text-muted-foreground">Rejected file</p>
        <FileDropzone
          accept=".xlsx"
          maxBytes={MAX_BYTES}
          hint={HINT}
          error="ferias-2027.csv isn't a .xlsx file. Download the template and fill it in."
        />
      </div>
      <div className="space-y-2">
        <p className="text-xs text-muted-foreground">Uploading</p>
        <FileDropzone
          accept=".xlsx"
          maxBytes={MAX_BYTES}
          hint={HINT}
          upload={{
            name: 'ferias-equipa-2027.xlsx',
            size: 184_320,
            progress: 64,
          }}
          onCancel={() => undefined}
        />
      </div>
    </div>
  );
}

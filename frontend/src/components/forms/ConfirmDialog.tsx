'use client';

import { Loader2 } from 'lucide-react';
import {
  type ReactElement,
  type ReactNode,
  useState,
  useTransition,
} from 'react';
import { toast } from 'sonner';

import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog';
import { FieldError } from '@/components/ui/field';
import type { ActionResult } from '@/lib/api/action-result';
import { errorMessage } from '@/lib/api/errors';

type ConfirmDialogProps = {
  title: string;
  description: ReactNode;
  confirmLabel: string;
  successMessage: string;
  onConfirm: () => Promise<ActionResult>;
  children: ReactElement;
};

export function ConfirmDialog({
  title,
  description,
  confirmLabel,
  successMessage,
  onConfirm,
  children,
}: ConfirmDialogProps) {
  const [open, setOpen] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isPending, startTransition] = useTransition();

  function handleOpenChange(nextOpen: boolean) {
    setOpen(nextOpen);
    setError(null);
  }

  function handleConfirm() {
    startTransition(async () => {
      let result: ActionResult;
      try {
        result = await onConfirm();
      } catch {
        setError(errorMessage('INTERNAL_ERROR'));
        return;
      }

      if (result.ok) {
        toast.success(successMessage);
        setOpen(false);
        return;
      }
      setError(result.error.message);
    });
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogTrigger asChild>{children}</DialogTrigger>
      <DialogContent role="alertdialog" showCloseButton={false}>
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
          <DialogDescription>{description}</DialogDescription>
        </DialogHeader>
        {error && <FieldError>{error}</FieldError>}
        <DialogFooter>
          <DialogClose asChild>
            <Button variant="outline">Cancel</Button>
          </DialogClose>
          <Button
            variant="destructive"
            disabled={isPending}
            onClick={handleConfirm}
          >
            {isPending && <Loader2 className="animate-spin" aria-hidden />}
            {confirmLabel}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

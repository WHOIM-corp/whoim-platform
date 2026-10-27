import { CircleAlert, X } from 'lucide-react'
import { useId, useState } from 'react'
import type { CardDetail } from '@/features/card/model/cardTypes.ts'
import { REJECT_REASON_MAX_LENGTH, rejectReasonError } from '@/features/card/model/rejectReason.ts'
import type { ApiError } from '@/shared/api/apiError.ts'
import { Button } from '@/shared/components/Button/Button.tsx'
import { Dialog } from '@/shared/components/Dialog/Dialog.tsx'
import { ErrorNotice } from '@/shared/components/ErrorNotice/ErrorNotice.tsx'
import styles from '@/features/card/components/RejectDialog/RejectDialog.module.scss'

type RejectDialogProps = {
  card: CardDetail
  isPending: boolean
  // 서버 오류(400·409). 그 사이 Card 상태가 바뀌어도 사용자가 닫을 때까지 보인다.
  error: ApiError | null
  onSubmit: (reason: string) => void
  onClose: () => void
}

// 열 때 마운트하고 닫을 때 언마운트해 입력이 다음 열기로 넘어가지 않게 한다. 요청은 useCardActions가 보낸다.
export function RejectDialog({ card, isPending, error, onSubmit, onClose }: RejectDialogProps) {
  const [reason, setReason] = useState('')
  const [fieldError, setFieldError] = useState<string | null>(null)
  const errorId = useId()

  function submit() {
    const validationError = rejectReasonError(reason)
    setFieldError(validationError)
    if (validationError === null) {
      onSubmit(reason.trim())
    }
  }

  return (
    <Dialog
      open
      title="Card 반려"
      description={`${card.title}을(를) 반려하면 Package의 활성 Card에서 빠지고 되돌릴 수 없습니다. 입력한 사유는 상태 이력에 남습니다.`}
      onClose={onClose}
      dismissible={!isPending}
    >
      {error && <ErrorNotice title="반려하지 못했습니다" message={error.message} />}
      <label className={styles.field}>
        <span>
          반려 사유 <small>(필수)</small>
        </span>
        <textarea
          className={styles.input}
          rows={3}
          maxLength={REJECT_REASON_MAX_LENGTH}
          value={reason}
          autoFocus
          placeholder="예) 근거 문서가 최신 절차와 달라 근거를 보강해야 합니다"
          aria-invalid={fieldError !== null}
          aria-describedby={fieldError !== null ? errorId : undefined}
          onChange={(event) => setReason(event.target.value)}
        />
        {fieldError !== null && (
          <span id={errorId} className={styles.error}>
            <CircleAlert size={14} aria-hidden="true" />
            {fieldError}
          </span>
        )}
      </label>
      <div className={styles.actions}>
        <Button onClick={onClose} disabled={isPending}>
          취소
        </Button>
        <Button variant="dangerSolid" onClick={submit} loading={isPending}>
          {!isPending && <X size={16} aria-hidden="true" />}
          반려
        </Button>
      </div>
    </Dialog>
  )
}

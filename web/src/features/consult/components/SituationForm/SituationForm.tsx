import { Send } from 'lucide-react'
import type { FormEvent, KeyboardEvent } from 'react'
import { CONSULT_LIMITS } from '@/features/consult/model/consultLimits.ts'
import { isCommitEnter } from '@/features/consult/model/enterKey.ts'
import { Button } from '@/shared/components/Button/Button.tsx'
import styles from '@/features/consult/components/SituationForm/SituationForm.module.scss'

type SituationFormProps = {
  value: string
  disabled: boolean
  onChange: (value: string) => void
  onSubmit: () => void
}

// Enter는 전송, Shift+Enter는 줄바꿈이다. 조합 중 Enter는 무시한다.
export function SituationForm({ value, disabled, onChange, onSubmit }: SituationFormProps) {
  const canSend = !disabled && value.trim() !== ''

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (canSend) {
      onSubmit()
    }
  }

  function sendOnEnter(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (!isCommitEnter(event)) {
      return
    }
    event.preventDefault()
    if (canSend) {
      onSubmit()
    }
  }

  return (
    <form className={styles.composer} onSubmit={submit}>
      <label className={styles.label} htmlFor="consult-situation">
        지금 마주한 상황
      </label>
      <textarea
        id="consult-situation"
        className={styles.input}
        rows={3}
        placeholder="예) 결제 성공률이 93%로 떨어졌어"
        value={value}
        maxLength={CONSULT_LIMITS.situation}
        disabled={disabled}
        onChange={(event) => onChange(event.target.value)}
        onKeyDown={sendOnEnter}
      />
      <div className={styles.actions}>
        <span className={styles.note}>입력한 내용은 저장되지 않습니다</span>
        <span className={styles.counter}>
          {value.length} / {CONSULT_LIMITS.situation}
        </span>
        <Button type="submit" variant="primary" disabled={!canSend}>
          <Send size={16} aria-hidden="true" />
          상황 전달
        </Button>
      </div>
    </form>
  )
}

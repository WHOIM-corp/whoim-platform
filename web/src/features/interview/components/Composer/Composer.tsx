import { Send } from 'lucide-react'
import type { FormEvent, KeyboardEvent } from 'react'
import { Button } from '@/shared/components/Button/Button.tsx'
import styles from '@/features/interview/components/Composer/Composer.module.scss'

type ComposerProps = {
  value: string
  maxLength: number
  disabled: boolean
  onChange: (value: string) => void
  onSubmit: () => void
  onUnknown: () => void
}

// 한글 조합 중 Enter(isComposing, keyCode 229)는 글자 확정용이라 보내지 않는다.
function isSendKey(event: KeyboardEvent<HTMLTextAreaElement>): boolean {
  return (
    event.key === 'Enter' &&
    !event.shiftKey &&
    !event.nativeEvent.isComposing &&
    event.keyCode !== 229
  )
}

export function Composer({
  value,
  maxLength,
  disabled,
  onChange,
  onSubmit,
  onUnknown,
}: ComposerProps) {
  const canSend = !disabled && value.trim() !== ''

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (canSend) {
      onSubmit()
    }
  }

  function sendOnEnter(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (!isSendKey(event)) {
      return
    }
    event.preventDefault()
    if (canSend) {
      onSubmit()
    }
  }

  return (
    <form className={styles.composer} onSubmit={submit}>
      <textarea
        className={styles.input}
        rows={3}
        aria-label="답변"
        placeholder="이 상황에서 실제로 어떻게 판단하셨는지 편하게 적어 주세요"
        value={value}
        maxLength={maxLength}
        disabled={disabled}
        onChange={(event) => onChange(event.target.value)}
        onKeyDown={sendOnEnter}
      />
      <div className={styles.actions}>
        <Button disabled={disabled} onClick={onUnknown}>
          모르겠다
        </Button>
        <span className={styles.counter}>
          {value.length} / {maxLength}
        </span>
        <Button type="submit" variant="primary" disabled={!canSend}>
          <Send size={16} aria-hidden="true" />
          답변 보내기
        </Button>
      </div>
    </form>
  )
}

import { Sparkles } from 'lucide-react'
import { useRef } from 'react'
import type { FormEvent, KeyboardEvent } from 'react'
import { CONSULT_LIMITS } from '@/features/consult/model/consultLimits.ts'
import { isCommitEnter } from '@/features/consult/model/enterKey.ts'
import { Button } from '@/shared/components/Button/Button.tsx'
import styles from '@/features/consult/components/FollowUpForm/FollowUpForm.module.scss'

type FollowUpFormProps = {
  questions: string[]
  answers: string[]
  disabled: boolean
  onChangeAnswer: (index: number, value: string) => void
  onSubmit: () => void
}

// 답은 비워 둘 수 있다(빈 답은 보내지 않는다). Enter는 다음 칸으로, 마지막 칸에서는 전달이다.
export function FollowUpForm({
  questions,
  answers,
  disabled,
  onChangeAnswer,
  onSubmit,
}: FollowUpFormProps) {
  const inputRefs = useRef<(HTMLInputElement | null)[]>([])

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!disabled) {
      onSubmit()
    }
  }

  function moveOnEnter(event: KeyboardEvent<HTMLInputElement>, index: number) {
    if (event.key !== 'Enter') {
      return
    }
    // 브라우저의 암묵적 폼 제출을 막고, 조합 중이 아닐 때만 직접 처리한다.
    event.preventDefault()
    if (!isCommitEnter(event)) {
      return
    }
    const next = inputRefs.current[index + 1]
    if (next) {
      next.focus()
    } else if (!disabled) {
      onSubmit()
    }
  }

  return (
    <form className={styles.followups} onSubmit={submit}>
      <p className={styles.heading}>
        <Sparkles size={14} aria-hidden="true" />
        전임자의 경험을 더 정확히 찾기 위한 질문
      </p>
      {questions.length === 0 ? (
        <p className={styles.empty}>추가로 확인할 질문이 없습니다.</p>
      ) : (
        questions.map((question, index) => (
          <label key={`${index}-${question}`} className={styles.field}>
            <span>{question}</span>
            <input
              ref={(element) => {
                inputRefs.current[index] = element
              }}
              className={styles.input}
              value={answers[index] ?? ''}
              maxLength={CONSULT_LIMITS.answer}
              disabled={disabled}
              placeholder="아는 만큼만 적어도 됩니다"
              onChange={(event) => onChangeAnswer(index, event.target.value)}
              onKeyDown={(event) => moveOnEnter(event, index)}
            />
          </label>
        ))
      )}
      <div className={styles.actions}>
        <Button type="submit" variant="primary" loading={disabled}>
          답변 전달
        </Button>
      </div>
    </form>
  )
}

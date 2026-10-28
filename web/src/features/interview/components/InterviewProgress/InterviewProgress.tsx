import { INTERVIEW_MAX_ANSWERED_TURNS } from '@/features/interview/model/traceFill.ts'
import styles from '@/features/interview/components/InterviewProgress/InterviewProgress.module.scss'

type InterviewProgressProps = {
  answeredCount: number
  isCompleted: boolean
}

// 진행 중에는 지금 답할 질문 번호를, 끝나면 답한 수를 보여준다.
export function InterviewProgress({ answeredCount, isCompleted }: InterviewProgressProps) {
  const current = isCompleted
    ? answeredCount
    : Math.min(answeredCount + 1, INTERVIEW_MAX_ANSWERED_TURNS)
  return (
    <div className={styles.progress}>
      <span>
        질문{' '}
        <b>
          {current} / {INTERVIEW_MAX_ANSWERED_TURNS}
        </b>
      </span>
      <span className={styles.bars} aria-hidden="true">
        {Array.from({ length: INTERVIEW_MAX_ANSWERED_TURNS }, (_, index) => {
          const isNow = !isCompleted && index === answeredCount
          const className = index < answeredCount ? styles.done : isNow ? styles.now : undefined
          return <span key={index} className={className} />
        })}
      </span>
      <span>최대 {INTERVIEW_MAX_ANSWERED_TURNS}턴</span>
    </div>
  )
}

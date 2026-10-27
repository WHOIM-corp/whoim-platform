import type { InterviewTurn } from '@/features/interview/model/interviewTypes.ts'
import styles from '@/features/interview/components/ChatLog/ChatLog.module.scss'

type ChatLogProps = {
  turns: InterviewTurn[]
  // 보냈지만 아직 turns에 들어오지 않은 답변이다.
  pendingAnswer: string | null
  isThinking: boolean
}

function renderAnswer(text: string) {
  return (
    <div className={styles.rowMe}>
      <div className={`${styles.bubble} ${styles.me}`}>{text}</div>
    </div>
  )
}

// 질문 라벨은 "질문 n"만 쓴다(질문은 자유 형식이라 TRACE 항목 정보가 없다).
export function ChatLog({ turns, pendingAnswer, isThinking }: ChatLogProps) {
  return (
    <div className={styles.log} aria-live="polite">
      {turns.map((turn) => (
        <div key={turn.turnNumber} className={styles.turn}>
          <p className={styles.label}>질문 {turn.turnNumber}</p>
          <div className={`${styles.bubble} ${styles.ai}`}>{turn.question}</div>
          {turn.answer !== null && renderAnswer(turn.answer)}
        </div>
      ))}
      {pendingAnswer !== null && renderAnswer(pendingAnswer)}
      {isThinking && (
        <div className={`${styles.bubble} ${styles.ai}`} role="status">
          <span className={styles.dots} aria-hidden="true">
            <i />
            <i />
            <i />
          </span>
          <span className={styles.thinking}>생각 중</span>
        </div>
      )}
    </div>
  )
}

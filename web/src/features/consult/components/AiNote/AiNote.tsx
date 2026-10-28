import { Sparkles } from 'lucide-react'
import styles from '@/features/consult/components/AiNote/AiNote.module.scss'

type AiNoteProps = {
  text: string
}

// AI가 쓴 글은 점선 유리 블록으로 Card 원문(흰 면)과 구분한다.
export function AiNote({ text }: AiNoteProps) {
  return (
    <aside className={styles.note} aria-label="AI 안내">
      <Sparkles className={styles.icon} size={16} aria-hidden="true" />
      <div>
        <b className={styles.label}>AI 안내</b>
        <p className={styles.text}>{text}</p>
      </div>
    </aside>
  )
}

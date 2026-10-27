import { splitByKeyword } from '@/features/member/lib/splitByKeyword.ts'
import styles from '@/features/member/components/MemberCombobox/MemberCombobox.module.scss'

type HighlightedTextProps = {
  text: string
  keyword: string
}

export function HighlightedText({ text, keyword }: HighlightedTextProps) {
  return (
    <>
      {splitByKeyword(text, keyword).map((segment, index) =>
        segment.matched ? (
          <mark key={index} className={styles.highlight}>
            {segment.text}
          </mark>
        ) : (
          <span key={index}>{segment.text}</span>
        ),
      )}
    </>
  )
}

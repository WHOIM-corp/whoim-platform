import { Check, CircleDashed, CircleDot, CircleQuestionMark, type LucideIcon } from 'lucide-react'
import { TRACE_GROUPS } from '@/features/card/model/traceGroups.ts'
import type { Interview } from '@/features/interview/model/interviewTypes.ts'
import { groupFillStatus, type GroupFillStatus } from '@/features/interview/model/traceFill.ts'
import styles from '@/features/interview/components/TraceFillPanel/TraceFillPanel.module.scss'

type TraceFillPanelProps = {
  interview: Interview
}

const FILL_STATUS_META: Record<GroupFillStatus, { label: string; icon: LucideIcon }> = {
  filled: { label: '채움', icon: Check },
  partial: { label: '일부', icon: CircleDot },
  unknown: { label: '모르겠다고 답함', icon: CircleQuestionMark },
  waiting: { label: '대기', icon: CircleDashed },
}

// TRACE 5그룹마다 두 항목의 인터뷰 채움 상태를 보여준다.
export function TraceFillPanel({ interview }: TraceFillPanelProps) {
  return (
    <section aria-label="TRACE 채움 현황">
      <h3 className={styles.heading}>TRACE 채움 현황</h3>
      <ul className={styles.list}>
        {TRACE_GROUPS.map((group) => {
          const status = groupFillStatus(interview, group)
          const { label, icon: Icon } = FILL_STATUS_META[status]
          return (
            <li key={group.letter} className={`${styles.item} ${styles[status]}`}>
              <span className={styles.letter}>{group.letter}</span>
              {group.label}
              <span className={styles.status}>
                <Icon size={14} strokeWidth={2} aria-hidden="true" />
                {label}
              </span>
            </li>
          )
        })}
      </ul>
    </section>
  )
}

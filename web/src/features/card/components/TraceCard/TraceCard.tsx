import { CheckCircle2, Circle } from 'lucide-react'
import { CardStatusBadge } from '@/features/card/components/CardStatusBadge/CardStatusBadge.tsx'
import { directEvidenceFor } from '@/features/card/model/evidenceByGroup.ts'
import type { CardDetail, CardStateHistory } from '@/features/card/model/cardTypes.ts'
import { emptyGroupCount, groupLines, TRACE_GROUPS, type TraceLetter } from '@/features/card/model/traceGroups.ts'
import { ApprovalBox, type ApprovalResult } from '@/shared/components/ApprovalBox/ApprovalBox.tsx'
import { formatShortDate } from '@/shared/lib/formatDate.ts'
import styles from '@/features/card/components/TraceCard/TraceCard.module.scss'

type TraceCardProps = {
  card: CardDetail
  activeGroup: TraceLetter | null
  onSelectGroup: (letter: TraceLetter) => void
}

function actorLabel(entry: CardStateHistory): string {
  return entry.actorName ?? entry.actorUsername ?? '시스템'
}

function latestEntryTo(history: CardStateHistory[], status: CardStateHistory['toStatus']) {
  return history.findLast((entry) => entry.toStatus === status)
}

function approvalResultOf(card: CardDetail): ApprovalResult {
  if (card.status !== 'APPROVED' && card.status !== 'REJECTED') {
    return { kind: 'waiting' }
  }
  const entry = latestEntryTo(card.stateHistory, card.status)
  return {
    kind: card.status === 'APPROVED' ? 'approved' : 'rejected',
    approverName: entry ? actorLabel(entry) : undefined,
    date: entry ? formatShortDate(entry.createdAt) : undefined,
    dateTime: entry?.createdAt,
  }
}

function confirmLine(card: CardDetail): string {
  if (card.status === 'DRAFT') {
    return '전임자 확인 전'
  }
  const entry = latestEntryTo(card.stateHistory, 'PREDECESSOR_CONFIRMED')
  if (!entry) {
    return '전임자 확인 완료'
  }
  return `전임자 ${actorLabel(entry)} 확인 ${formatShortDate(entry.createdAt)}`
}

// 3:4 유리 베젤 TRACE Card다. 그룹 행을 눌러 우측 근거 패널의 대상을 고른다.
export function TraceCard({ card, activeGroup, onSelectGroup }: TraceCardProps) {
  const confirmed = card.status !== 'DRAFT'
  const ConfirmIcon = confirmed ? CheckCircle2 : Circle
  return (
    <article className={styles.card} aria-label={card.title}>
      <div className={styles.face}>
        <header className={styles.head}>
          <span className={styles.kind}>TRACE Card</span>
          <CardStatusBadge status={card.status} />
        </header>
        <h2 className={styles.title}>{card.title}</h2>
        <ol className={styles.rows}>
          {TRACE_GROUPS.map((group) => {
            const lines = groupLines(card, group)
            const directCount = directEvidenceFor(card, group).length
            return (
              <li key={group.letter} className={styles.item}>
                <button
                  type="button"
                  className={styles.row}
                  aria-pressed={activeGroup === group.letter}
                  onClick={() => onSelectGroup(group.letter)}
                >
                  <span className={styles.letter}>{group.letter}</span>
                  <span className={styles.content}>
                    <span className={styles.itemLabel}>
                      {group.label}
                      <em>{group.english}</em>
                      <span className={styles.chip}>발언 {directCount}</span>
                    </span>
                    {lines.length === 0 ? (
                      <span className={styles.empty}>전임자 답변이 없어 비어 있습니다</span>
                    ) : (
                      lines.map((line) => (
                        <span key={line.label} className={styles.line}>
                          <span className={styles.lineLabel}>{line.label}</span>
                          <span className={styles.text}>{line.text}</span>
                        </span>
                      ))
                    )}
                  </span>
                </button>
              </li>
            )
          })}
        </ol>
        <footer className={styles.foot}>
          <div>
            <p className={confirmed ? styles.done : undefined}>
              <ConfirmIcon size={14} strokeWidth={2} aria-hidden="true" />
              {confirmLine(card)}
            </p>
            <p>
              근거 <b>{card.evidenceLinks.length}건</b> · 비어 있는 요소 <b>{emptyGroupCount(card)}개</b>
            </p>
          </div>
          <ApprovalBox result={approvalResultOf(card)} />
        </footer>
      </div>
    </article>
  )
}

import { FileText, MessageSquareQuote } from 'lucide-react'
import type { ReactNode } from 'react'
import { directEvidenceFor, recordedEvidence } from '@/features/card/model/evidenceByGroup.ts'
import type { CardDetail, CardEvidenceLink } from '@/features/card/model/cardTypes.ts'
import { TRACE_GROUPS, type TraceLetter } from '@/features/card/model/traceGroups.ts'
import { GlassPanel } from '@/shared/components/GlassPanel/GlassPanel.tsx'
import styles from '@/features/card/components/EvidencePanel/EvidencePanel.module.scss'

type EvidencePanelProps = {
  card: CardDetail
  activeGroup: TraceLetter | null
  // 패널 머리의 오른쪽에 둘 요소다 (예: 근거 | 이력 탭).
  actions?: ReactNode
}

function locationOf(link: CardEvidenceLink): string {
  if (link.type === 'DIRECT') {
    return link.turnNumber === null ? '' : `질문 ${link.turnNumber}`
  }
  const file = link.fileName ?? '문서'
  return link.startOffset === null || link.endOffset === null
    ? file
    : `${file} · ${link.startOffset}-${link.endOffset}`
}

function EvidenceItem({ link }: { link: CardEvidenceLink }) {
  const direct = link.type === 'DIRECT'
  const Icon = direct ? MessageSquareQuote : FileText
  const location = locationOf(link)
  return (
    <li className={`${styles.src} ${direct ? styles.direct : styles.recorded}`}>
      <div className={styles.srcHead}>
        <span className={styles.srcType}>
          <Icon size={14} strokeWidth={2} aria-hidden="true" />
          {direct ? '전임자 발언' : '업무 기록'}
        </span>
        {location !== '' && <span className={styles.loc}>{location}</span>}
      </div>
      <q className={styles.quote}>{link.excerpt}</q>
    </li>
  )
}

// 선택한 TRACE 항목의 발언 근거와, Card 전체에 붙은 문서 근거를 두 구역으로 보여준다.
export function EvidencePanel({ card, activeGroup, actions }: EvidencePanelProps) {
  const group = TRACE_GROUPS.find((candidate) => candidate.letter === activeGroup)
  const direct = group === undefined ? [] : directEvidenceFor(card, group)
  const recorded = recordedEvidence(card)
  return (
    <GlassPanel
      title="근거"
      subtitle={group === undefined ? 'TRACE 항목을 선택하세요' : `${group.letter} · ${group.label}`}
      actions={actions}
    >
      <section className={styles.section} aria-label="선택한 항목의 발언 근거">
        <h3 className={styles.heading}>선택한 항목의 발언 근거</h3>
        {direct.length === 0 ? (
          <p className={styles.empty}>
            {group === undefined ? '항목을 선택하면 발언 근거가 보입니다' : '이 항목에 연결된 발언이 없습니다'}
          </p>
        ) : (
          <ul className={styles.list}>
            {direct.map((link, index) => (
              <EvidenceItem key={`${link.turnNumber}-${index}`} link={link} />
            ))}
          </ul>
        )}
      </section>
      <section className={styles.section} aria-label="이 Card의 문서 근거">
        <h3 className={styles.heading}>이 Card의 문서 근거</h3>
        {recorded.length === 0 ? (
          <p className={styles.empty}>연결된 문서 근거가 없습니다</p>
        ) : (
          <ul className={styles.list}>
            {recorded.map((link, index) => (
              <EvidenceItem key={`${link.documentId}-${link.startOffset}-${index}`} link={link} />
            ))}
          </ul>
        )}
      </section>
    </GlassPanel>
  )
}

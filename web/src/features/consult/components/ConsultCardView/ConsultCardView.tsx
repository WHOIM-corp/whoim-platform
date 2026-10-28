import {
  ChevronDown,
  ChevronUp,
  FileText,
  MessageSquareQuote,
  Radar,
  Route,
  Scale,
  ShieldCheck,
  TriangleAlert,
} from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import { useId, useState } from 'react'
import { groupLines, TRACE_GROUPS, type TraceLetter } from '@/features/card/model/traceGroups.ts'
import { evidenceCountsOf } from '@/features/consult/model/consultEvidence.ts'
import type { ConsultCard, ConsultEvidence } from '@/features/consult/model/consultTypes.ts'
import { Button } from '@/shared/components/Button/Button.tsx'
import { formatShortDate } from '@/shared/lib/formatDate.ts'
import styles from '@/features/consult/components/ConsultCardView/ConsultCardView.module.scss'

type ConsultCardViewProps = {
  card: ConsultCard
}

type SummaryBlock = { letter: TraceLetter; title: string; icon: LucideIcon }

// 요약 면은 원인(R)을 빼고 네 블록만 보여준다. 원인은 "자세히 보기"에서 펼친다.
const SUMMARY_BLOCKS: SummaryBlock[] = [
  { letter: 'T', title: '당시 상황', icon: Radar },
  { letter: 'A', title: '전임자의 판단', icon: Scale },
  { letter: 'C', title: '선택한 행동', icon: Route },
  { letter: 'E', title: '예외와 위험', icon: TriangleAlert },
]

function linesOf(card: ConsultCard, letter: TraceLetter) {
  const group = TRACE_GROUPS.find((candidate) => candidate.letter === letter)
  return group === undefined ? [] : groupLines(card, group)
}

function dateOrPending(iso: string | null): string {
  return iso === null ? '기록 없음' : formatShortDate(iso)
}

function EvidenceItem({ evidence }: { evidence: ConsultEvidence }) {
  const direct = evidence.type === 'DIRECT'
  const Icon = direct ? MessageSquareQuote : FileText
  return (
    <li className={styles.evidence}>
      <span className={styles.evidenceHead}>
        <Icon size={14} strokeWidth={2} aria-hidden="true" />
        {direct ? '발언' : '문서'}
        {evidence.fileName !== null && <span className={styles.file}>{evidence.fileName}</span>}
      </span>
      <q className={styles.quote}>{evidence.excerpt}</q>
    </li>
  )
}

// 상담 결과의 Card 원문(흰 면)이다. AI 설명은 이 컴포넌트 밖의 AiNote가 맡는다.
export function ConsultCardView({ card }: ConsultCardViewProps) {
  const [expanded, setExpanded] = useState(false)
  const detailId = useId()
  const { direct, recorded } = evidenceCountsOf(card)
  const total = direct + recorded
  const { predecessorConfirmedAt, approvedAt } = card.verification
  const ToggleIcon = expanded ? ChevronUp : ChevronDown

  return (
    <article className={styles.card} aria-label={`전임자의 TRACE Card: ${card.title}`}>
      <header className={styles.head}>
        <span className={styles.kind}>TRACE Card</span>
        <span className={styles.verify}>
          <ShieldCheck size={14} aria-hidden="true" />
          전임자 확인 {dateOrPending(predecessorConfirmedAt)} · 승인자 승인{' '}
          {dateOrPending(approvedAt)}
        </span>
      </header>
      <div className={styles.body}>
        <h2 className={styles.title}>{card.title}</h2>
        <div className={styles.grid}>
          {SUMMARY_BLOCKS.map(({ letter, title, icon: Icon }) => {
            const lines = linesOf(card, letter)
            return (
              <section key={letter} className={styles.block}>
                <h3>
                  <Icon size={14} aria-hidden="true" />
                  {title}
                </h3>
                {lines.length === 0 ? (
                  <p className={styles.empty}>기록 없음</p>
                ) : (
                  lines.map((line) => (
                    <p key={line.label}>
                      <span className={styles.lineLabel}>{line.label}</span>
                      {line.text}
                    </p>
                  ))
                )}
              </section>
            )
          })}
        </div>
        <div className={styles.meterBlock}>
          <span
            className={styles.meter}
            role="img"
            aria-label={`발언 근거 ${direct}건, 문서 근거 ${recorded}건`}
          >
            {total === 0 ? (
              <span className={styles.none} />
            ) : (
              <>
                <span className={styles.direct} style={{ flexGrow: direct }} />
                <span className={styles.recorded} style={{ flexGrow: recorded }} />
              </>
            )}
          </span>
          <span className={styles.legend}>
            <span>
              <MessageSquareQuote size={12} aria-hidden="true" />
              발언 {direct}
            </span>
            <span>
              <FileText size={12} aria-hidden="true" />
              문서 {recorded}
            </span>
            <span>근거 {total}건</span>
          </span>
        </div>
      </div>
      {expanded && (
        <div id={detailId} className={styles.detail}>
          <ol className={styles.groups}>
            {TRACE_GROUPS.map((group) => {
              const lines = groupLines(card, group)
              return (
                <li key={group.letter} className={styles.group}>
                  <span className={styles.letter}>{group.letter}</span>
                  <div>
                    <p className={styles.groupLabel}>
                      {group.label}
                      <em>{group.english}</em>
                    </p>
                    {lines.length === 0 ? (
                      <p className={styles.empty}>기록 없음</p>
                    ) : (
                      lines.map((line) => (
                        <p key={line.label} className={styles.groupLine}>
                          <span className={styles.lineLabel}>{line.label}</span>
                          {line.text}
                        </p>
                      ))
                    )}
                  </div>
                </li>
              )
            })}
          </ol>
          <section aria-label="이 Card의 근거">
            <h3 className={styles.detailHeading}>근거 {total}건</h3>
            {total === 0 ? (
              <p className={styles.empty}>연결된 근거가 없습니다</p>
            ) : (
              <ul className={styles.evidences}>
                {card.evidences.map((evidence, index) => (
                  <EvidenceItem key={`${evidence.type}-${index}`} evidence={evidence} />
                ))}
              </ul>
            )}
          </section>
        </div>
      )}
      <footer className={styles.foot}>
        <span>공개된 Package의 승인된 Card</span>
        <Button
          aria-expanded={expanded}
          aria-controls={detailId}
          onClick={() => setExpanded((value) => !value)}
        >
          {expanded ? '판단 과정 접기' : '판단 과정 자세히 보기'}
          <ToggleIcon size={16} aria-hidden="true" />
        </Button>
      </footer>
    </article>
  )
}

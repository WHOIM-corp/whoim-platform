import { useState } from 'react'
import { EvidencePanel } from '@/features/card/components/EvidencePanel/EvidencePanel.tsx'
import { HistoryTimeline } from '@/features/card/components/HistoryTimeline/HistoryTimeline.tsx'
import type { CardDetail } from '@/features/card/model/cardTypes.ts'
import type { TraceLetter } from '@/features/card/model/traceGroups.ts'
import { GlassPanel } from '@/shared/components/GlassPanel/GlassPanel.tsx'
import { SegmentedControl } from '@/shared/components/SegmentedControl/SegmentedControl.tsx'

type PanelTab = 'evidence' | 'history'

type CardSidePanelProps = {
  card: CardDetail
  activeGroup: TraceLetter | null
  // 이력 탭은 승인자와 전임자만 본다.
  withHistory: boolean
}

const PANEL_TABS: { value: PanelTab; label: string }[] = [
  { value: 'evidence', label: '근거' },
  { value: 'history', label: '이력' },
]

export function CardSidePanel({ card, activeGroup, withHistory }: CardSidePanelProps) {
  const [tab, setTab] = useState<PanelTab>('evidence')
  const tabs = withHistory ? (
    <SegmentedControl items={PANEL_TABS} value={tab} onChange={setTab} ariaLabel="우측 패널 보기" />
  ) : undefined

  if (withHistory && tab === 'history') {
    return (
      <GlassPanel title="상태 이력" subtitle={`기록 ${card.stateHistory.length}건`} actions={tabs}>
        <HistoryTimeline history={card.stateHistory} />
      </GlassPanel>
    )
  }
  return <EvidencePanel card={card} activeGroup={activeGroup} actions={tabs} />
}

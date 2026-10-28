import type { CardDetail, CardEvidenceLink } from '@/features/card/model/cardTypes.ts'
import type { TraceGroup } from '@/features/card/model/traceGroups.ts'

// 발언 근거 중 이 그룹의 TRACE 항목에서 나온 것. 한 발언이 여러 그룹에 걸치면 모두에 나온다.
export function directEvidenceFor(card: CardDetail, group: TraceGroup): CardEvidenceLink[] {
  return card.evidenceLinks.filter(
    (link) => link.type === 'DIRECT' && link.elements.some((element) => group.elements.includes(element)),
  )
}

// 문서 근거는 항목과 연결되지 않고 Card 전체에 붙는다.
export function recordedEvidence(card: CardDetail): CardEvidenceLink[] {
  return card.evidenceLinks.filter((link) => link.type === 'RECORDED')
}

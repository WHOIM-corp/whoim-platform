import type { HandoverPackage } from '@/features/handover/model/handoverTypes.ts'

// 서버 HandoverProgress.publishable()과 같은 규칙이다. 최종 판단은 서버가 한다.
export function publishBlockReason(handoverPackage: HandoverPackage): string | null {
  if (handoverPackage.status !== 'WRITING') {
    return '이미 공개된 Package입니다.'
  }
  if (handoverPackage.successor === null) {
    return '후임자를 지정해야 공개할 수 있습니다.'
  }
  const { activeCount, approvedCount } = handoverPackage.progress
  if (activeCount === 0) {
    return '공개할 Card가 없습니다.'
  }
  if (approvedCount !== activeCount) {
    return '모든 Card가 승인되어야 공개할 수 있습니다.'
  }
  return null
}

export function canPublish(handoverPackage: HandoverPackage): boolean {
  return publishBlockReason(handoverPackage) === null
}

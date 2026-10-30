import type { Position } from '@/features/auth/model/authTypes.ts'

const ALL_POSITIONS: readonly Position[] = ['PREDECESSOR', 'SUCCESSOR', 'REVIEWER']

export const ROUTE_ACCESS = {
  home: ALL_POSITIONS,
  packages: ALL_POSITIONS,
  packageDetail: ALL_POSITIONS,
  packageNew: ['REVIEWER'],
  packageEvidence: ['PREDECESSOR', 'REVIEWER'],
  packageGaps: ['PREDECESSOR'],
  packageGapInterview: ['PREDECESSOR'],
  cardDetail: ALL_POSITIONS,
  queue: ['PREDECESSOR', 'REVIEWER'],
  consult: ['SUCCESSOR'],
} as const satisfies Record<string, readonly Position[]>

export const ROUTE_PATHS = {
  login: '/login',
  home: '/home',
  packages: '/packages',
  packageNew: '/packages/new',
  packageDetail: '/packages/:packageId',
  packageGaps: '/packages/:packageId/gaps',
  packageGapInterview: '/packages/:packageId/gaps/:gapId/interview',
  queue: '/queue',
  consult: '/consult',
  cardDetail: '/cards/:cardId',
} as const

export type ConnectedSourceKey = 'JIRA' | 'GIT' | 'WIKI'

export type ConnectedSource = {
  key: ConnectedSourceKey
  name: string
  scope: string
  scopeDetail: string
}

// 서버와 연결되지 않은 합성 표시 데이터다. 연동 범위 문구는 승인된 mock 값이다.
export const CONNECTED_SOURCES: ConnectedSource[] = [
  { key: 'JIRA', name: 'Jira', scope: '프로젝트 PAY', scopeDetail: '이슈 본문·코멘트 · 최근 6개월' },
  { key: 'GIT', name: 'Git', scope: '저장소 payment-core', scopeDetail: 'PR 본문·리뷰 코멘트 · 최근 6개월' },
  { key: 'WIKI', name: 'Wiki', scope: '스페이스 PAY', scopeDetail: '운영·장애 대응 페이지 · 최근 6개월' },
]

export const EXTENDABLE_SOURCES: string[] = ['KMS', '사내 RAG']

export const EXTENDABLE_SOURCE_HINT = '같은 Connector 인터페이스로 추가할 수 있습니다'
export const DLP_NOTICE = '외부로 전달되는 자료는 민감정보 마스킹(DLP)을 거칩니다.'

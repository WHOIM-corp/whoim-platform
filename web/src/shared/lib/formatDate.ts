// 서버가 +09:00 오프셋 문자열(yyyy-MM-ddTHH:mm:ss...)을 주므로 시간대 변환 없이 글자 그대로 읽는다.
const ISO_PATTERN = /^\d{4}-(\d{2})-(\d{2})T(\d{2}):(\d{2})/

export function formatShortDate(iso: string): string {
  const match = ISO_PATTERN.exec(iso)
  return match === null ? iso : `${match[1]}.${match[2]}`
}

export function formatDateTime(iso: string): string {
  const match = ISO_PATTERN.exec(iso)
  return match === null ? iso : `${match[1]}.${match[2]} ${match[3]}:${match[4]}`
}

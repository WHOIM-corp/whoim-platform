export type TextSegment = { text: string; matched: boolean }

// 검색어와 일치하는 부분을 모두 나눈다. 사용자 문자열은 HTML이 아니라 텍스트 노드로만 그린다.
export function splitByKeyword(text: string, keyword: string): TextSegment[] {
  const needle = keyword.trim().toLowerCase()
  const haystack = text.toLowerCase()
  if (needle === '' || haystack.length !== text.length) {
    return [{ text, matched: false }]
  }

  const segments: TextSegment[] = []
  let cursor = 0
  let found = haystack.indexOf(needle, cursor)
  while (found !== -1) {
    if (found > cursor) {
      segments.push({ text: text.slice(cursor, found), matched: false })
    }
    segments.push({ text: text.slice(found, found + needle.length), matched: true })
    cursor = found + needle.length
    found = haystack.indexOf(needle, cursor)
  }
  if (cursor < text.length) {
    segments.push({ text: text.slice(cursor), matched: false })
  }
  return segments
}

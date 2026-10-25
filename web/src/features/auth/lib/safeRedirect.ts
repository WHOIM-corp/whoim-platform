// 로그인 뒤 이동 경로는 앱 내부 경로만 허용한다(//host, 스킴, 역슬래시는 외부 이동으로 간주).
export function isInternalPath(path: string): boolean {
  return /^\/(?![/\\])/.test(path) && !path.includes('\\')
}

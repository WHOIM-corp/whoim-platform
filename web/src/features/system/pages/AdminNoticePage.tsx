import { ShieldCheck } from 'lucide-react'
import { useLogout } from '@/features/auth/hooks/useLogout.ts'
import { Button } from '@/shared/components/Button/Button.tsx'
import { StateView } from '@/shared/components/StateView/StateView.tsx'

export function AdminNoticePage() {
  const logout = useLogout()

  return (
    <StateView
      icon={ShieldCheck}
      title="관리자 계정은 웹 업무 화면이 없습니다"
      message="관리자 콘솔은 별도 주소에서 사용합니다. 업무 화면을 보려면 전임자, 승인자, 후임자 계정으로 로그인해 주세요."
      action={<Button onClick={logout}>로그아웃</Button>}
    />
  )
}

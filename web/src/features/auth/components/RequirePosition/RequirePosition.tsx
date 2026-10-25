import { Outlet } from 'react-router'
import { useAuthStore } from '@/features/auth/model/authStore.ts'
import type { Position } from '@/features/auth/model/authTypes.ts'
import { ForbiddenPage } from '@/features/system/pages/ForbiddenPage.tsx'

type RequirePositionProps = {
  allow: readonly Position[]
}

export function RequirePosition({ allow }: RequirePositionProps) {
  const position = useAuthStore((state) => state.member?.position ?? null)
  return position !== null && allow.includes(position) ? <Outlet /> : <ForbiddenPage />
}

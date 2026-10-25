import { Outlet } from 'react-router'
import { useAuthStore } from '@/features/auth/model/authStore.ts'
import { AdminNoticePage } from '@/features/system/pages/AdminNoticePage.tsx'

export function AdminGate() {
  const role = useAuthStore((state) => state.member?.role)
  return role === 'ADMIN' ? <AdminNoticePage /> : <Outlet />
}

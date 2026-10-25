import { useNavigate } from 'react-router'
import { ROUTE_PATHS } from '@/app/routePaths.ts'
import { useAuthStore } from '@/features/auth/model/authStore.ts'

export function useLogout(): () => void {
  const signOut = useAuthStore((state) => state.signOut)
  const navigate = useNavigate()

  return function logout(): void {
    signOut()
    navigate(ROUTE_PATHS.login, { replace: true })
  }
}

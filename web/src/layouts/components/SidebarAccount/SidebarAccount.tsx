import { LogOut } from 'lucide-react'
import { useLogout } from '@/features/auth/hooks/useLogout.ts'
import { useAuthStore } from '@/features/auth/model/authStore.ts'
import { positionLabels } from '@/features/auth/model/authTypes.ts'
import styles from '@/layouts/components/SidebarAccount/SidebarAccount.module.scss'

const ADMIN_LABEL = '관리자'
const LOGOUT_LABEL = '로그아웃'

export function SidebarAccount() {
  const member = useAuthStore((state) => state.member)
  const logout = useLogout()

  if (member === null) {
    return null
  }

  return (
    <div className={styles.account}>
      <span className={styles.avatar} aria-hidden="true">
        {member.name.charAt(0)}
      </span>
      <div className={styles.identity}>
        <b className={styles.name}>{member.name}</b>
        <span className={styles.role}>
          {member.position === null ? ADMIN_LABEL : positionLabels[member.position]}
        </span>
      </div>
      <button
        type="button"
        className={styles.logout}
        aria-label={LOGOUT_LABEL}
        title={LOGOUT_LABEL}
        onClick={logout}
      >
        <LogOut size={16} strokeWidth={1.5} aria-hidden="true" />
      </button>
    </div>
  )
}

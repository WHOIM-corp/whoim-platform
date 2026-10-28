import type { MemberSearchResult } from '@/features/member/model/memberTypes.ts'
import { Button } from '@/shared/components/Button/Button.tsx'
import styles from '@/features/handover/components/PersonSlot/PersonSlot.module.scss'

type PersonSlotProps = {
  member: MemberSearchResult
  onClear: () => void
}

export function PersonSlot({ member, onClear }: PersonSlotProps) {
  return (
    <div className={styles.person}>
      <span className={styles.avatar} aria-hidden="true">
        {member.name.charAt(0)}
      </span>
      <span className={styles.identity}>
        <b className={styles.name}>{member.name}</b>
        <small className={styles.detail}>
          {member.department} · {member.jobTitle} · {member.employeeNumber}
        </small>
      </span>
      <Button className={styles.clear} onClick={onClear} aria-label={`${member.name} 선택 변경`}>
        변경
      </Button>
    </div>
  )
}

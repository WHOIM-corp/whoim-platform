import { ArrowRight, Plus } from 'lucide-react'
import { PackageStatusBadge } from '@/features/handover/components/PackageStatusBadge/PackageStatusBadge.tsx'
import type { MemberSearchResult } from '@/features/member/model/memberTypes.ts'
import { Button } from '@/shared/components/Button/Button.tsx'
import { GlassPanel } from '@/shared/components/GlassPanel/GlassPanel.tsx'
import styles from '@/features/handover/components/PackagePreview/PackagePreview.module.scss'

const NEXT_STEPS = ['문서 업로드', 'Gap 분석', '인터뷰', 'Card 확인']

type PackagePreviewProps = {
  formId: string
  name: string
  description: string
  predecessor: MemberSearchResult | null
  successor: MemberSearchResult | null
  isPending: boolean
  onCancel: () => void
}

type PreviewPersonProps = {
  label: string
  member: MemberSearchResult | null
}

function PreviewPerson({ label, member }: PreviewPersonProps) {
  return (
    <span className={member === null ? `${styles.person} ${styles.empty}` : styles.person}>
      <span className={styles.avatar} aria-hidden="true">
        {member === null ? '?' : member.name.charAt(0)}
      </span>
      <span>
        <small className={styles.personLabel}>{label}</small>
        <b className={styles.personName}>{member === null ? '선택 전' : member.name}</b>
      </span>
    </span>
  )
}

export function PackagePreview({
  formId,
  name,
  description,
  predecessor,
  successor,
  isPending,
  onCancel,
}: PackagePreviewProps) {
  const trimmedName = name.trim()
  const trimmedDescription = description.trim()
  return (
    <GlassPanel title="미리보기" subtitle="입력한 내용이 이렇게 만들어집니다">
      <div className={styles.card}>
        <div className={styles.top}>
          <b className={trimmedName === '' ? `${styles.name} ${styles.unset}` : styles.name}>
            {trimmedName === '' ? '이름을 입력하면 여기에 보입니다' : trimmedName}
          </b>
          <PackageStatusBadge status="WRITING" />
        </div>
        <p className={styles.description}>{trimmedDescription === '' ? '설명 없음' : trimmedDescription}</p>
        <div className={styles.people}>
          <PreviewPerson label="전임자" member={predecessor} />
          <ArrowRight className={styles.arrow} size={16} strokeWidth={1.5} aria-hidden="true" />
          <PreviewPerson label="후임자" member={successor} />
        </div>
      </div>
      <section className={styles.flow}>
        <h3 className={styles.flowTitle}>만든 뒤 진행</h3>
        <ol className={styles.steps}>
          {NEXT_STEPS.map((label, index) => (
            <li key={label} className={styles.step}>
              <span className={styles.dot}>{index + 1}</span>
              {label}
            </li>
          ))}
        </ol>
      </section>
      <div className={styles.actions}>
        <Button onClick={onCancel} disabled={isPending}>
          취소
        </Button>
        <Button type="submit" form={formId} variant="primary" loading={isPending}>
          {!isPending && <Plus size={16} aria-hidden="true" />}
          Package 만들기
        </Button>
      </div>
    </GlassPanel>
  )
}

import { CircleAlert } from 'lucide-react'
import { useEffect, useId, useRef } from 'react'
import type { ReactNode } from 'react'
import { useNavigate } from 'react-router'
import { ROUTE_PATHS } from '@/app/routePaths.ts'
import { PackagePreview } from '@/features/handover/components/PackagePreview/PackagePreview.tsx'
import {
  PACKAGE_DESCRIPTION_MAX_LENGTH,
  PACKAGE_NAME_MAX_LENGTH,
  useNewPackageForm,
} from '@/features/handover/hooks/useNewPackageForm.ts'
import { MemberCombobox } from '@/features/member/components/MemberCombobox/MemberCombobox.tsx'
import { ErrorNotice } from '@/shared/components/ErrorNotice/ErrorNotice.tsx'
import { PageHeader } from '@/shared/components/PageHeader/PageHeader.tsx'
import styles from '@/features/handover/pages/NewPackagePage.module.scss'

function FieldError({ id, message }: { id: string; message?: string }) {
  if (message === undefined) {
    return null
  }
  return (
    <span id={id} className={styles.error}>
      <CircleAlert size={14} strokeWidth={1.5} aria-hidden="true" />
      {message}
    </span>
  )
}

function FormSection({
  title,
  hint,
  children,
}: {
  title: string
  hint: string
  children: ReactNode
}) {
  return (
    <section className={styles.section}>
      <h2 className={styles.sectionTitle}>{title}</h2>
      <p className={styles.sectionHint}>{hint}</p>
      {children}
    </section>
  )
}

export function NewPackagePage() {
  const navigate = useNavigate()
  const form = useNewPackageForm()
  const formId = useId()
  const formRef = useRef<HTMLFormElement>(null)
  const nameErrorId = useId()
  const descriptionErrorId = useId()
  const predecessorErrorId = useId()
  const successorErrorId = useId()

  // 검증이 실패하면 첫 오류 칸으로 포커스를 옮긴다.
  useEffect(() => {
    if (form.invalidAttempt > 0) {
      formRef.current?.querySelector<HTMLElement>('[aria-invalid="true"]')?.focus()
    }
  }, [form.invalidAttempt])

  return (
    <>
      <PageHeader
        title="새 Package"
        meta="한 명의 전임자가 남긴 판단을 한 명의 후임자에게 전달하는 묶음입니다"
      />
      <div className={styles.stage}>
        <form
          id={formId}
          ref={formRef}
          className={styles.form}
          noValidate
          onSubmit={form.handleSubmit}
        >
          {form.serverError && (
            <ErrorNotice title="Package를 만들지 못했습니다" message={form.serverError.message} />
          )}
          <FormSection title="기본 정보" hint="후임자 화면과 승인 목록에 이 이름으로 보입니다">
            <label className={styles.field}>
              <span>
                이름 <small>(필수)</small>
              </span>
              <input
                className={styles.input}
                value={form.name}
                maxLength={PACKAGE_NAME_MAX_LENGTH}
                placeholder="예) 정산·대사 인수인계"
                aria-invalid={form.errors.name !== undefined}
                aria-describedby={form.errors.name !== undefined ? nameErrorId : undefined}
                onChange={(event) => form.setName(event.target.value)}
              />
              <FieldError id={nameErrorId} message={form.errors.name} />
            </label>
            <label className={styles.field}>
              <span>
                설명 <small>(선택)</small>
              </span>
              <textarea
                className={styles.input}
                rows={2}
                value={form.description}
                maxLength={PACKAGE_DESCRIPTION_MAX_LENGTH}
                aria-invalid={form.errors.description !== undefined}
                aria-describedby={form.errors.description !== undefined ? descriptionErrorId : undefined}
                placeholder="이 Package가 다루는 업무 범위를 적어 두면 인터뷰와 검수에서 기준이 됩니다"
                onChange={(event) => form.setDescription(event.target.value)}
              />
              <FieldError id={descriptionErrorId} message={form.errors.description} />
            </label>
          </FormSection>
          <FormSection
            title="전임자"
            hint="문서를 올리고 인터뷰에 답하며 Card를 확인하는 사람입니다"
          >
            <div className={styles.slot}>
              <MemberCombobox
                position="PREDECESSOR"
                value={form.predecessor}
                onChange={form.setPredecessor}
                ariaLabel="전임자 검색"
                invalid={form.errors.predecessor !== undefined}
              />
            </div>
            <FieldError id={predecessorErrorId} message={form.errors.predecessor} />
          </FormSection>
          <FormSection title="후임자" hint="공개된 뒤 상담으로 전임자의 경험을 보게 될 사람입니다">
            <div className={styles.slot}>
              <MemberCombobox
                position="SUCCESSOR"
                value={form.successor}
                onChange={form.setSuccessor}
                ariaLabel="후임자 검색"
                invalid={form.errors.successor !== undefined}
              />
            </div>
            <FieldError id={successorErrorId} message={form.errors.successor} />
          </FormSection>
        </form>
        <div className={styles.preview}>
          <PackagePreview
            formId={formId}
            name={form.name}
            description={form.description}
            predecessor={form.predecessor}
            successor={form.successor}
            isPending={form.isPending}
            onCancel={() => navigate(ROUTE_PATHS.packages)}
          />
        </div>
      </div>
    </>
  )
}

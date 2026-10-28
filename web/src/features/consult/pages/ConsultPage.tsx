import { RotateCcw } from 'lucide-react'
import { useAuthStore } from '@/features/auth/model/authStore.ts'
import { ConsultThread } from '@/features/consult/components/ConsultThread/ConsultThread.tsx'
import { useConsultFlow } from '@/features/consult/hooks/useConsultFlow.ts'
import { Button } from '@/shared/components/Button/Button.tsx'
import { PageHeader } from '@/shared/components/PageHeader/PageHeader.tsx'

export function ConsultPage() {
  const flow = useConsultFlow()
  const memberName = useAuthStore((state) => state.member?.name ?? null)

  return (
    <>
      <PageHeader
        title="상담"
        meta="공개된 Package의 승인된 Card에서 전임자의 경험을 찾아 보여줍니다"
        actions={
          flow.step !== 'situation' && (
            <Button disabled={flow.isPending} onClick={flow.reset}>
              <RotateCcw size={16} aria-hidden="true" />새 상담
            </Button>
          )
        }
      />
      <ConsultThread flow={flow} memberName={memberName} />
    </>
  )
}

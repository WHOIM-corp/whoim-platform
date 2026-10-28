import { Layers, Plus } from 'lucide-react'
import { useNavigate } from 'react-router'
import { ROUTE_PATHS } from '@/app/routePaths.ts'
import { useAuthStore } from '@/features/auth/model/authStore.ts'
import { CARD_QUEUE_BY_POSITION } from '@/features/card/model/cardQueue.ts'
import { PackageTable } from '@/features/handover/components/PackageTable/PackageTable.tsx'
import { useHandoverPackages } from '@/features/handover/hooks/useHandoverPackages.ts'
import { Button } from '@/shared/components/Button/Button.tsx'
import { CardSkeletonGrid } from '@/shared/components/CardSkeletonGrid/CardSkeletonGrid.tsx'
import { ErrorNotice } from '@/shared/components/ErrorNotice/ErrorNotice.tsx'
import { PageHeader } from '@/shared/components/PageHeader/PageHeader.tsx'
import { StateView } from '@/shared/components/StateView/StateView.tsx'

export function PackageListPage() {
  const navigate = useNavigate()
  const position = useAuthStore((state) => state.member?.position ?? null)
  const packagesQuery = useHandoverPackages()
  const packages = packagesQuery.data

  function renderBody() {
    if (packagesQuery.isLoading) {
      return <CardSkeletonGrid />
    }
    if (packagesQuery.error !== null) {
      return (
        <ErrorNotice
          title="Package를 불러오지 못했습니다"
          message={packagesQuery.error.message}
          actionLabel="다시 시도"
          onAction={packagesQuery.refetch}
        />
      )
    }
    if (packages === undefined || packages.length === 0) {
      return position === 'SUCCESSOR' ? (
        <StateView icon={Layers} title="아직 공개된 Package가 없습니다" message="공개되면 여기에 나타납니다." />
      ) : (
        <StateView icon={Layers} title="Package가 없습니다" message="Package가 만들어지면 여기에 나타납니다." />
      )
    }
    return (
      <PackageTable packages={packages} queue={position === null ? undefined : CARD_QUEUE_BY_POSITION[position]} />
    )
  }

  return (
    <>
      <PageHeader
        title="Package"
        meta={
          position === 'SUCCESSOR'
            ? '공개된 Package만 보입니다'
            : packages && `${packages.length}개`
        }
        actions={
          position === 'REVIEWER' && (
            <Button variant="primary" onClick={() => navigate(ROUTE_PATHS.packageNew)}>
              <Plus size={16} aria-hidden="true" />새 Package
            </Button>
          )
        }
      />
      {renderBody()}
    </>
  )
}

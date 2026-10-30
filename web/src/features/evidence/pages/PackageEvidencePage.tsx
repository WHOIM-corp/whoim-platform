import { useParams } from 'react-router'
import { useAuthStore } from '@/features/auth/model/authStore.ts'
import { ConnectedSourcesPanel } from '@/features/evidence/components/ConnectedSourcesPanel/ConnectedSourcesPanel.tsx'
import { DocumentUploadForm } from '@/features/evidence/components/DocumentUploadForm/DocumentUploadForm.tsx'
import { EvidenceDocumentList } from '@/features/evidence/components/EvidenceDocumentList/EvidenceDocumentList.tsx'
import { EvidenceLockNotice } from '@/features/evidence/components/EvidenceLockNotice/EvidenceLockNotice.tsx'
import { useEvidenceDocuments } from '@/features/evidence/hooks/useEvidenceDocuments.ts'
import { evidenceLockReason } from '@/features/evidence/model/evidenceAccess.ts'
import { PackageTabs } from '@/features/gap/components/PackageTabs/PackageTabs.tsx'
import { useGaps } from '@/features/gap/hooks/useGaps.ts'
import { PackageStatusBadge } from '@/features/handover/components/PackageStatusBadge/PackageStatusBadge.tsx'
import { useHandoverPackage } from '@/features/handover/hooks/useHandoverPackage.ts'
import { QueryErrorView } from '@/features/system/components/QueryErrorView/QueryErrorView.tsx'
import { CardSkeletonGrid } from '@/shared/components/CardSkeletonGrid/CardSkeletonGrid.tsx'
import { ErrorNotice } from '@/shared/components/ErrorNotice/ErrorNotice.tsx'
import { PageHeader } from '@/shared/components/PageHeader/PageHeader.tsx'
import styles from '@/features/evidence/pages/PackageEvidencePage.module.scss'

export function PackageEvidencePage() {
  const { packageId = '' } = useParams()
  const position = useAuthStore((state) => state.member?.position ?? null)
  const isPredecessor = position === 'PREDECESSOR'
  const packageQuery = useHandoverPackage(packageId)
  // 승인자도 호출은 하지만 결과는 무시한다. Gap 데이터·오류·로딩은 전임자일 때만 판단에 쓴다.
  const gapsQuery = useGaps(packageId)
  const documentsQuery = useEvidenceDocuments(packageId)

  if (packageQuery.error !== null) {
    return (
      <QueryErrorView error={packageQuery.error} title="Package를 불러오지 못했습니다" onRetry={packageQuery.refetch} />
    )
  }
  const handoverPackage = packageQuery.data

  function refetchStaleLock() {
    gapsQuery.refetch()
    packageQuery.refetch()
  }

  function renderDocuments(canUpload: boolean) {
    if (documentsQuery.isLoading) {
      return <CardSkeletonGrid />
    }
    if (documentsQuery.error !== null) {
      return (
        <ErrorNotice
          title="근거 자료를 불러오지 못했습니다"
          message={documentsQuery.error.message}
          actionLabel="다시 시도"
          onAction={documentsQuery.refetch}
        />
      )
    }
    return <EvidenceDocumentList documents={documentsQuery.data ?? []} canUpload={canUpload} />
  }

  function renderBody() {
    // 분석 시작 여부를 알기 전에는 폼이 켜지지 않도록 스켈레톤을 둔다.
    if (handoverPackage === undefined || (isPredecessor && gapsQuery.data === undefined && gapsQuery.error === null)) {
      return <CardSkeletonGrid />
    }
    if (isPredecessor && gapsQuery.error !== null) {
      return (
        <ErrorNotice
          title="Gap 분석 여부를 확인하지 못했습니다"
          message={gapsQuery.error.message}
          actionLabel="다시 시도"
          onAction={gapsQuery.refetch}
        />
      )
    }
    const lockReason = evidenceLockReason({
      position,
      status: handoverPackage.status,
      gapCount: gapsQuery.data?.length ?? 0,
    })
    const canUpload = lockReason === null
    return (
      <div className={styles.body}>
        {canUpload ? (
          <DocumentUploadForm packageId={packageId} onStale={refetchStaleLock} />
        ) : (
          <EvidenceLockNotice reason={lockReason} />
        )}
        {canUpload && <ConnectedSourcesPanel />}
        {renderDocuments(canUpload)}
      </div>
    )
  }

  return (
    <>
      <PageHeader
        title={handoverPackage?.name ?? 'Package'}
        meta={handoverPackage && <PackageStatusBadge status={handoverPackage.status} />}
      />
      {isPredecessor && <PackageTabs packageId={packageId} active="evidence" />}
      {renderBody()}
    </>
  )
}

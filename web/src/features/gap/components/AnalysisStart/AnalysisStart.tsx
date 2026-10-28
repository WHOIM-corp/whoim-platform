import { LoaderCircle, ScanSearch, Sparkles } from 'lucide-react'
import { useState } from 'react'
import { useAnalyzeGaps } from '@/features/gap/hooks/useAnalyzeGaps.ts'
import type { ApiError } from '@/shared/api/apiError.ts'
import { Button } from '@/shared/components/Button/Button.tsx'
import { ErrorNotice } from '@/shared/components/ErrorNotice/ErrorNotice.tsx'
import { StateView } from '@/shared/components/StateView/StateView.tsx'
import styles from '@/features/gap/components/AnalysisStart/AnalysisStart.module.scss'

type AnalysisStartProps = {
  packageId: string
  // 이미 분석된 Package(409)면 화면의 목록이 낡았으므로 다시 불러온다.
  onStale: () => void
}

function analysisErrorMessage(error: ApiError): string {
  if (error.errorCode === 'GAP_ANALYSIS_NO_DOCUMENTS') {
    return '분석할 문서가 없습니다. 문서를 올린 뒤 다시 시도할 수 있습니다'
  }
  if (error.errorCode === 'LLM_UNAVAILABLE') {
    return 'AI 응답이 지연되고 있습니다'
  }
  return error.message
}

export function AnalysisStart({ packageId, onStale }: AnalysisStartProps) {
  const analyzeGaps = useAnalyzeGaps()
  const [foundNothing, setFoundNothing] = useState(false)

  function analyze() {
    setFoundNothing(false)
    analyzeGaps.submit(packageId, {
      onSuccess: (gaps) => setFoundNothing(gaps.length === 0),
      onError: (apiError) => {
        if (apiError.status === 409) {
          onStale()
        }
      },
    })
  }

  if (analyzeGaps.isPending) {
    return (
      <StateView
        icon={LoaderCircle}
        title="생각 중입니다"
        message="문서에서 판단의 공백을 찾고 있습니다. 잠시만 기다려 주세요."
      />
    )
  }

  const error = analyzeGaps.error
  return (
    <>
      {error !== null && (
        <div className={styles.notice}>
          <ErrorNotice
            title="분석하지 못했습니다"
            message={analysisErrorMessage(error)}
            actionLabel={error.status === 503 ? '다시 시도' : undefined}
            onAction={analyze}
          />
        </div>
      )}
      {foundNothing && (
        <p className={styles.info}>찾은 판단 공백이 없습니다. 문서를 확인한 뒤 다시 시도할 수 있습니다</p>
      )}
      <StateView
        icon={ScanSearch}
        title="아직 분석하지 않은 Package입니다"
        message="업로드한 문서에서 판단의 공백(Gap)을 찾습니다. 분석은 Package당 한 번만 실행할 수 있고 성공하면 이 버튼은 사라집니다."
        action={
          <Button variant="primary" onClick={analyze}>
            <Sparkles size={16} aria-hidden="true" />
            분석 시작
          </Button>
        }
      />
    </>
  )
}

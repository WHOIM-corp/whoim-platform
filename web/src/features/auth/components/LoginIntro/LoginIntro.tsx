import { ShieldCheck } from 'lucide-react'
import { ServiceFlowList } from '@/features/auth/components/ServiceFlowList/ServiceFlowList.tsx'
import { TracePreviewCard } from '@/features/auth/components/TracePreviewCard/TracePreviewCard.tsx'
import styles from '@/features/auth/components/LoginIntro/LoginIntro.module.scss'

export function LoginIntro() {
  return (
    <section className={styles.intro} aria-label="서비스 소개">
      <p className={styles.wordmark}>WHO I'M</p>
      <h2 className={styles.headline}>시니어의 판단을, 근거와 함께 이어받습니다.</h2>
      <p className={styles.lede}>
        인수인계 문서에 남지 않는 업무 판단을 AI 인터뷰로 복원하고, 근거가 연결된 TRACE Card로
        검증해 후임자가 실제 업무에서 쓰게 합니다.
      </p>
      <div className={styles.showcase}>
        <ServiceFlowList />
        <TracePreviewCard />
      </div>
      <p className={styles.foot}>
        <ShieldCheck size={16} strokeWidth={1.5} aria-hidden="true" />이 환경은 합성 데이터만
        사용하는 시연용입니다
      </p>
    </section>
  )
}

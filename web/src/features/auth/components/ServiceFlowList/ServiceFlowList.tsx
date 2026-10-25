import { SERVICE_FLOW_STEPS } from '@/features/auth/components/ServiceFlowList/serviceFlowSteps.ts'
import styles from '@/features/auth/components/ServiceFlowList/ServiceFlowList.module.scss'

export function ServiceFlowList() {
  return (
    <ol className={styles.flow} aria-label="서비스 흐름">
      {SERVICE_FLOW_STEPS.map(({ icon: Icon, title, description }) => (
        <li key={title} className={styles.step}>
          <span className={styles.stepIcon}>
            <Icon size={16} strokeWidth={1.5} aria-hidden="true" />
          </span>
          <div>
            <b className={styles.stepTitle}>{title}</b>
            <span className={styles.stepDescription}>{description}</span>
          </div>
        </li>
      ))}
    </ol>
  )
}

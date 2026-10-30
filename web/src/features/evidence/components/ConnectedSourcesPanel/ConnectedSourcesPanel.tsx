import { Lock, Plug, ShieldCheck } from 'lucide-react'
import {
  CONNECTED_SOURCES,
  DLP_NOTICE,
  EXTENDABLE_SOURCES,
  EXTENDABLE_SOURCE_HINT,
} from '@/features/evidence/model/connectedSources.ts'
import styles from '@/features/evidence/components/ConnectedSourcesPanel/ConnectedSourcesPanel.module.scss'

export function ConnectedSourcesPanel() {
  return (
    <section className={styles.section} aria-labelledby="connected-sources-heading">
      <header className={styles.header}>
        <h2 id="connected-sources-heading" className={styles.heading}>
          <Plug size={16} aria-hidden="true" />
          연동 소스
        </h2>
        <span className={styles.hint}>읽기 전용</span>
      </header>
      <ul className={styles.sources}>
        {CONNECTED_SOURCES.map((source) => (
          <li
            key={source.key}
            className={styles.source}
            aria-label={`${source.name} 연동 소스: 연결됨, 읽기 전용`}
          >
            <div className={styles.top}>
              <span className={styles.glyph} aria-hidden="true">
                {source.name.charAt(0)}
              </span>
              <strong>{source.name}</strong>
              <span className={styles.connected}>
                <span aria-hidden="true">●</span>연결됨
              </span>
            </div>
            <dl className={styles.details}>
              <div className={styles.pair}>
                <dt>연동 범위</dt>
                <dd>
                  {source.scope}
                  <small>{source.scopeDetail}</small>
                </dd>
              </div>
              <div className={styles.pair}>
                <dt>접근 권한</dt>
                <dd className={styles.readOnly}>읽기 전용</dd>
              </div>
            </dl>
          </li>
        ))}
      </ul>
      <div className={styles.extendable}>
        <span className={styles.extendableLabel}>
          <Lock size={12} aria-hidden="true" />
          확장 가능한 소스
        </span>
        {EXTENDABLE_SOURCES.map((name) => (
          <span key={name} className={styles.chip} title={EXTENDABLE_SOURCE_HINT}>
            {name}
            <span>확장 가능</span>
          </span>
        ))}
      </div>
      <p className={styles.secure}>
        <ShieldCheck size={14} aria-hidden="true" />
        <span>{DLP_NOTICE}</span>
      </p>
    </section>
  )
}

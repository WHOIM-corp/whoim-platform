import type { LucideIcon } from 'lucide-react'
import { useId } from 'react'
import type { InputHTMLAttributes, ReactNode } from 'react'
import styles from '@/features/auth/components/TextField/TextField.module.scss'

type TextFieldProps = Omit<InputHTMLAttributes<HTMLInputElement>, 'className'> & {
  label: string
  icon: LucideIcon
  error?: string
  trailing?: ReactNode
}

export function TextField({ label, icon: Icon, error, trailing, ...inputProps }: TextFieldProps) {
  const errorId = useId()
  return (
    <label className={styles.field}>
      {label}
      <span className={styles.control}>
        <Icon className={styles.icon} size={16} strokeWidth={1.5} aria-hidden="true" />
        <input
          {...inputProps}
          className={trailing ? `${styles.input} ${styles.withTrailing}` : styles.input}
          aria-invalid={error !== undefined}
          aria-describedby={error !== undefined ? errorId : undefined}
        />
        {trailing}
      </span>
      {error !== undefined && (
        <span id={errorId} className={styles.error}>
          {error}
        </span>
      )}
    </label>
  )
}

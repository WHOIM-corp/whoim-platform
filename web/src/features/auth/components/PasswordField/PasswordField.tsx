import { Eye, EyeOff, LockKeyhole } from 'lucide-react'
import { useState } from 'react'
import { TextField } from '@/features/auth/components/TextField/TextField.tsx'
import styles from '@/features/auth/components/PasswordField/PasswordField.module.scss'

type PasswordFieldProps = {
  label: string
  value: string
  placeholder: string
  error?: string
  onChange: (value: string) => void
}

export function PasswordField({ label, value, placeholder, error, onChange }: PasswordFieldProps) {
  const [revealed, setRevealed] = useState(false)
  const ToggleIcon = revealed ? EyeOff : Eye
  return (
    <TextField
      label={label}
      icon={LockKeyhole}
      type={revealed ? 'text' : 'password'}
      autoComplete="current-password"
      placeholder={placeholder}
      value={value}
      error={error}
      onChange={(event) => onChange(event.target.value)}
      trailing={
        <button
          type="button"
          className={styles.toggle}
          aria-label={revealed ? '비밀번호 숨기기' : '비밀번호 보기'}
          onClick={() => setRevealed((current) => !current)}
        >
          <ToggleIcon size={16} strokeWidth={1.5} aria-hidden="true" />
        </button>
      }
    />
  )
}

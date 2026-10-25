import { Info, UserRound } from 'lucide-react'
import type { FormEvent } from 'react'
import { PasswordField } from '@/features/auth/components/PasswordField/PasswordField.tsx'
import { TextField } from '@/features/auth/components/TextField/TextField.tsx'
import type { LoginRequest } from '@/features/auth/model/authTypes.ts'
import type { ApiError } from '@/shared/api/apiError.ts'
import { Button } from '@/shared/components/Button/Button.tsx'
import { ErrorNotice } from '@/shared/components/ErrorNotice/ErrorNotice.tsx'
import styles from '@/features/auth/components/LoginForm/LoginForm.module.scss'

type LoginFormProps = {
  values: LoginRequest
  errors: { username?: string; password?: string }
  isPending: boolean
  serverError: ApiError | null
  onChange: (field: keyof LoginRequest, value: string) => void
  onSubmit: (event: FormEvent) => void
}

export function LoginForm({
  values,
  errors,
  isPending,
  serverError,
  onChange,
  onSubmit,
}: LoginFormProps) {
  return (
    <>
      <form className={styles.form} noValidate onSubmit={onSubmit}>
        {serverError && <ErrorNotice message={serverError.message} />}
        <TextField
          label="아이디"
          icon={UserRound}
          type="text"
          autoComplete="username"
          placeholder="아이디를 입력해 주세요"
          value={values.username}
          error={errors.username}
          onChange={(event) => onChange('username', event.target.value)}
        />
        <PasswordField
          label="비밀번호"
          placeholder="비밀번호를 입력해 주세요"
          value={values.password}
          error={errors.password}
          onChange={(value) => onChange('password', value)}
        />
        <Button type="submit" variant="primary" className={styles.submit} loading={isPending}>
          로그인
        </Button>
      </form>
      <p className={styles.notice}>
        <Info className={styles.noticeIcon} size={16} strokeWidth={1.5} aria-hidden="true" />
        전임자, 승인자, 후임자 계정으로 로그인하면 역할에 맞는 화면으로 이동합니다.
      </p>
    </>
  )
}

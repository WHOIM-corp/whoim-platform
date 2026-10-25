import { useState } from 'react'
import type { FormEvent } from 'react'
import type { LoginRequest } from '@/features/auth/model/authTypes.ts'

type LoginFormErrors = { username?: string; password?: string }

type UseLoginFormResult = {
  values: LoginRequest
  errors: LoginFormErrors
  setField: (field: keyof LoginRequest, value: string) => void
  handleSubmit: (event: FormEvent) => void
}

export function useLoginForm(onSubmit: (request: LoginRequest) => void): UseLoginFormResult {
  const [values, setValues] = useState<LoginRequest>({ username: '', password: '' })
  const [errors, setErrors] = useState<LoginFormErrors>({})

  function setField(field: keyof LoginRequest, value: string): void {
    setValues((current) => ({ ...current, [field]: value }))
    setErrors((current) => ({ ...current, [field]: undefined }))
  }

  function handleSubmit(event: FormEvent): void {
    event.preventDefault()
    const nextErrors: LoginFormErrors = {}
    if (values.username === '') {
      nextErrors.username = '아이디를 입력해 주세요'
    }
    if (values.password === '') {
      nextErrors.password = '비밀번호를 입력해 주세요'
    }
    setErrors(nextErrors)
    if (nextErrors.username === undefined && nextErrors.password === undefined) {
      onSubmit(values)
    }
  }

  return { values, errors, setField, handleSubmit }
}

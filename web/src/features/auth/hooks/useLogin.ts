import { useMutation } from '@tanstack/react-query'
import { login } from '@/features/auth/api/loginApi.ts'
import { useAuthStore } from '@/features/auth/model/authStore.ts'
import type { LoginRequest } from '@/features/auth/model/authTypes.ts'
import { ApiError, toApiError } from '@/shared/api/apiError.ts'

type UseLoginResult = {
  submit: (request: LoginRequest) => void
  isPending: boolean
  error: ApiError | null
}

export function useLogin(): UseLoginResult {
  const signIn = useAuthStore((state) => state.signIn)

  const mutation = useMutation({
    mutationFn: login,
    // 로그인 뒤 이동은 LoginPage가 로그인 상태를 보고 한 번만 한다.
    onSuccess: (response) => signIn(response),
  })

  function submit(request: LoginRequest): void {
    if (mutation.isPending) {
      return
    }
    mutation.mutate(request)
  }

  const error =
    mutation.error === null
      ? null
      : mutation.error instanceof ApiError
        ? mutation.error
        : toApiError(0, undefined)

  return { submit, isPending: mutation.isPending, error }
}

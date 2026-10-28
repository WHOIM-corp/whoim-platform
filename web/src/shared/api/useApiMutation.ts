import { useRef } from 'react'
import { useMutation } from '@tanstack/react-query'
import type { ApiError } from '@/shared/api/apiError.ts'
import { normalizeApiError } from '@/shared/api/normalizeApiError.ts'

type SubmitOptions<Data> = {
  onSuccess?: (data: Data) => void
  // 409처럼 화면 데이터가 낡았을 때 호출하는 쪽이 다시 불러오는 곳이다.
  onError?: (error: ApiError) => void
}

export type ApiMutationResult<Variables, Data> = {
  submit: (variables: Variables, options?: SubmitOptions<Data>) => void
  isPending: boolean
  error: ApiError | null
  reset: () => void
}

type ApiMutationConfig<Variables, Data> = {
  mutationFn: (variables: Variables) => Promise<Data>
  // 성공 시 관련 쿼리를 무효화하는 곳이다.
  onSuccess?: (data: Data, variables: Variables) => Promise<unknown> | void
}

// 진행 중에는 submit을 무시해 이중 제출을 막고, 오류는 ApiError로 통일한다.
export function useApiMutation<Variables, Data>(
  config: ApiMutationConfig<Variables, Data>,
): ApiMutationResult<Variables, Data> {
  // isPending은 다시 그린 뒤에야 바뀌므로, 같은 틱의 두 번째 클릭은 ref로 막는다.
  const isSubmittingRef = useRef(false)
  const mutation = useMutation({
    mutationFn: config.mutationFn,
    onSuccess: config.onSuccess,
    onSettled: () => {
      isSubmittingRef.current = false
    },
  })

  function submit(variables: Variables, options?: SubmitOptions<Data>): void {
    if (isSubmittingRef.current) {
      return
    }
    isSubmittingRef.current = true
    mutation.mutate(variables, {
      onSuccess: options?.onSuccess,
      onError: (error) => {
        const apiError = normalizeApiError(error)
        if (apiError !== null) {
          options?.onError?.(apiError)
        }
      },
    })
  }

  return {
    submit,
    isPending: mutation.isPending,
    error: normalizeApiError(mutation.error),
    reset: mutation.reset,
  }
}

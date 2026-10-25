import { Navigate, useLocation } from 'react-router'
import { LoginCard } from '@/features/auth/components/LoginCard/LoginCard.tsx'
import { LoginForm } from '@/features/auth/components/LoginForm/LoginForm.tsx'
import { LoginIntro } from '@/features/auth/components/LoginIntro/LoginIntro.tsx'
import { useLogin } from '@/features/auth/hooks/useLogin.ts'
import { useLoginForm } from '@/features/auth/hooks/useLoginForm.ts'
import { resolveRedirectTarget } from '@/features/auth/lib/locationState.ts'
import { useAuthStore } from '@/features/auth/model/authStore.ts'
import { AuthLayout } from '@/layouts/AuthLayout/AuthLayout.tsx'
import { ErrorNotice } from '@/shared/components/ErrorNotice/ErrorNotice.tsx'

export function LoginPage() {
  const member = useAuthStore((state) => state.member)
  const sessionExpired = useAuthStore((state) => state.sessionExpired)
  const location = useLocation()
  const { submit, isPending, error } = useLogin()
  const { values, errors, setField, handleSubmit } = useLoginForm(submit)

  if (member !== null) {
    return <Navigate to={resolveRedirectTarget(location.state, member.position)} replace />
  }

  return (
    <AuthLayout intro={<LoginIntro />}>
      <LoginCard
        notice={
          sessionExpired && <ErrorNotice message="세션이 만료되었습니다. 다시 로그인해 주세요." />
        }
      >
        <LoginForm
          values={values}
          errors={errors}
          isPending={isPending}
          serverError={error}
          onChange={setField}
          onSubmit={handleSubmit}
        />
      </LoginCard>
    </AuthLayout>
  )
}

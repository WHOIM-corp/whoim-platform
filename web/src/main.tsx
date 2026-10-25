import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import 'pretendard/dist/web/variable/pretendardvariable-dynamic-subset.css'
import '@/shared/styles/global.scss'
import { App } from '@/app/App.tsx'
import { syncQueryCacheWithAuth } from '@/app/authCacheSync.ts'
import { configureApi } from '@/app/configureApi.ts'

configureApi()
syncQueryCacheWithAuth()

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)

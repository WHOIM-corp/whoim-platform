import { useParams } from 'react-router'
import { CardDetailView } from '@/features/card/components/CardDetailView/CardDetailView.tsx'

export function CardDetailPage() {
  const { cardId = '' } = useParams()
  return <CardDetailView key={cardId} cardId={cardId} />
}

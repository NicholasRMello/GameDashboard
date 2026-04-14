export type Currency = 'BRL' | 'USD' | 'EUR'

export type ApiGoal = {
  id: number
  title: string
  estimatedHours: number
  hoursPerDay: number
  daysPerWeek: number
  price: number
  currency: Currency
  createdAt: string
}

type CreateGoalPayload = {
  title: string
  estimatedHours: number
  hoursPerDay: number
  daysPerWeek: number
  price: number
  currency: Currency
}

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL as string | undefined)?.trim() || 'http://localhost:8080/api/v1'

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...(init?.headers ?? {}),
    },
  })

  if (!response.ok) {
    throw new Error(`API ${response.status}: ${response.statusText}`)
  }

  if (response.status === 204) {
    return undefined as T
  }

  return (await response.json()) as T
}

export async function listGoals(): Promise<ApiGoal[]> {
  return request<ApiGoal[]>('/goals')
}

export async function createGoal(payload: CreateGoalPayload): Promise<ApiGoal> {
  return request<ApiGoal>('/goals', {
    method: 'POST',
    body: JSON.stringify({
      title: payload.title,
      estimatedHours: payload.estimatedHours,
      hoursPerDay: payload.hoursPerDay,
      daysPerWeek: payload.daysPerWeek,
      price: payload.price,
      currency: payload.currency,
      imageUrl: null,
      rawgId: null,
      released: null,
      rating: null,
      genres: [],
      description: null,
    }),
  })
}

export async function deleteGoal(goalId: number): Promise<void> {
  await request<void>(`/goals/${goalId}`, {
    method: 'DELETE',
  })
}

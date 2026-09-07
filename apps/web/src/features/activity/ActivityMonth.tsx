import { createContext, useContext, useState, type ReactNode } from 'react'
import { seoulToday } from '@todorok/client-domain'
const currentMonth = () => seoulToday().slice(0, 7)
const shift = (month: string, amount: number) => { const [year, index] = month.split('-').map(Number); const date = new Date(Date.UTC(year, index - 1 + amount, 1)); return `${date.getUTCFullYear()}-${String(date.getUTCMonth() + 1).padStart(2, '0')}` }
const Context = createContext<{ month: string; previous: () => void; next: () => void; atCurrent: boolean } | null>(null)
export function ActivityMonthProvider({ children }: { children: ReactNode }) { const [month, setMonth] = useState(currentMonth); return <Context.Provider value={{ month, previous: () => setMonth((value) => shift(value, -1)), next: () => setMonth((value) => value < currentMonth() ? shift(value, 1) : value), atCurrent: month >= currentMonth() }}>{children}</Context.Provider> }
export function useActivityMonth() { const value = useContext(Context); if (!value) throw new Error('ActivityMonthProvider is required'); return value }

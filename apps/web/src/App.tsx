import { colors } from '@todorok/design-tokens'
import type { CSSProperties } from 'react'
import './styles.css'

const appStyle = {
  '--color-background': colors.background,
  '--color-surface': colors.surface,
  '--color-text': colors.text,
  '--color-accent': colors.accent,
} as CSSProperties

export function App() {
  return (
    <main className="app-shell" style={appStyle}>
      <section className="welcome-card" aria-labelledby="product-name">
        <p className="eyebrow">오늘을 계획하고 기록하는 한곳</p>
        <h1 id="product-name">토도록</h1>
        <p className="description">
          할 일과 일정에 운동, 공부, 클라이밍 기록을 자연스럽게 연결합니다.
        </p>
      </section>
    </main>
  )
}

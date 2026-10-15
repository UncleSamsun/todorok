import { useEffect, useLayoutEffect, useRef, useState, type ReactNode } from 'react'

export function TaskFloatingPanel({ children, close, busy, label }: { children: ReactNode; close: () => void; busy: boolean; label: string }) {
  const root = useRef<HTMLDivElement>(null)
  const [position, setPosition] = useState({left:16,top:16})
  useEffect(() => {
    const dismiss = (event: PointerEvent) => {
      if (!busy && !root.current?.contains(event.target as Node)) close()
    }
    document.addEventListener('pointerdown',dismiss)
    return () => document.removeEventListener('pointerdown',dismiss)
  }, [busy,close])
  useLayoutEffect(() => {
    const panel = root.current
    const anchor = panel?.parentElement
    if (!panel || !anchor) return
    const place = () => {
      const row = anchor.getBoundingClientRect(), box = panel.getBoundingClientRect()
      setPosition({left:Math.max(16,Math.min(row.right-box.width,innerWidth-box.width-16)),
        top:Math.max(12,Math.min(row.bottom+4,innerHeight-box.height-88))})
    }
    place()
    const observer = typeof ResizeObserver === 'undefined' ? null : new ResizeObserver(place)
    observer?.observe(panel)
    window.addEventListener('resize',place)
    window.addEventListener('scroll',place,true)
    return () => { observer?.disconnect(); window.removeEventListener('resize',place); window.removeEventListener('scroll',place,true) }
  }, [])
  return <div ref={root} className="task-floating-panel" role="dialog" aria-label={label} style={position}
    onKeyDown={event => { if (event.key === 'Escape' && !busy) { event.preventDefault(); close() } }}>
    <header><strong>{label}</strong></header>
    {children}
  </div>
}

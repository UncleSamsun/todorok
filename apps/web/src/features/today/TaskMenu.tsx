import { useEffect, useRef, useState } from 'react'
import type { Task } from './model'

export function TaskMenu({ task, busy, edit }: { task: Task; busy: boolean; edit: (mode: 'task' | 'repeat' | 'memo') => void }) {
  const [open, setOpen] = useState(false)
  const root = useRef<HTMLDivElement>(null)
  const toggle = useRef<HTMLButtonElement>(null)
  const item = useRef<HTMLButtonElement>(null)
  useEffect(() => {
    if (!open) return
    item.current?.focus()
    function outside(event: PointerEvent) {
      if (!root.current?.contains(event.target as Node)) setOpen(false)
    }
    document.addEventListener('pointerdown', outside)
    return () => document.removeEventListener('pointerdown', outside)
  }, [open])
  return <div className="task-menu" ref={root} onKeyDown={event => {
    if (event.key === 'Escape') { event.preventDefault(); setOpen(false); toggle.current?.focus() }
    if (event.key === 'Tab') setOpen(false)
  }}>
    <button ref={toggle} type="button" className="task-menu-toggle" aria-label={`${task.title} 메뉴`}
      aria-haspopup="menu" aria-expanded={open} disabled={busy} onClick={() => setOpen(!open)}>…</button>
    {open && <div className="task-menu-popup" role="menu" aria-label={`${task.title} 옵션`}>
      <button ref={item} type="button" role="menuitem" disabled={busy} onClick={() => { setOpen(false); edit('task') }}>일정 수정</button>
      <button type="button" role="menuitem" disabled={busy || (!task.seriesId && (task.taskType !== 'GENERAL' || task.status !== 'PLANNED'))} onClick={() => { setOpen(false); edit('repeat') }}>반복 설정</button>
      <button type="button" role="menuitem" disabled={busy} onClick={() => { setOpen(false); edit('memo') }}>메모</button>
    </div>}
  </div>
}

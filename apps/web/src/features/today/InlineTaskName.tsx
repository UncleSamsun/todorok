import { useRef, useState } from 'react'
import type { Task } from './model'

export function InlineTaskName({ task, save, close, busy }: {
  task: Task; save: (task: Task, title: string) => Promise<boolean>; close: () => void; busy: boolean
}) {
  const [title,setTitle]=useState(task.title)
  const pending=useRef(false)
  async function commit() {
    if (pending.current || busy) return
    if (!title.trim() || title.trim()===task.title) { close(); return }
    pending.current=true
    try { if(await save(task,title.trim())) close() }
    finally { pending.current=false }
  }
  return <span className="inline-name-wrap"><input autoFocus readOnly={busy} aria-label={`${task.title} 이름`} value={title} maxLength={120}
    onChange={event=>setTitle(event.target.value)} onBlur={()=>void commit()} onKeyDown={event=>{
      if(event.key==='Enter'&&!event.nativeEvent.isComposing) {event.preventDefault();void commit()}
      if(event.key==='Escape') {event.preventDefault();pending.current=true;close()}
    }} /></span>
}

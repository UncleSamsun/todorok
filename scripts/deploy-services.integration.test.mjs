import assert from 'node:assert/strict'
import { spawnSync } from 'node:child_process'
import { mkdtemp, writeFile, rm } from 'node:fs/promises'
import path from 'node:path'
import os from 'node:os'
import test from 'node:test'

test('a failed migration job leaves the running release container unchanged',async()=>{
  const dir=await mkdtemp(path.join(os.tmpdir(),'todorok-deploy-rehearsal-'))
  const project=`todorok-deploy-rehearsal-${process.pid}-${Date.now()}`
  const base=path.join(dir,'before.yml'),next=path.join(dir,'next.yml'),env=path.join(dir,'.env')
  const docker=(...args)=>{const r=spawnSync('docker',args,{encoding:'utf8',timeout:60000});assert.equal(r.status,0,r.stderr);return r.stdout.trim()}
  const compose=file=>['compose','-p',project,'--env-file',env,'-f',file]
  try{
    await writeFile(env,`COMPOSE_PROJECT_NAME=${project}\n`)
    await writeFile(base,'services:\n  planner-service:\n    image: node:24-alpine\n    command: ["node", "-e", "setInterval(()=>{},1000)"]\n    labels:\n      rehearsal.version: old\n')
    const otherRuntime=['activity-service','notification-service','nginx'].map(name=>`  ${name}:\n    image: node:24-alpine\n    command: ["node", "-e", "setInterval(()=>{},1000)"]\n`).join('')
    const otherMigrations=['activity-migration','notification-migration'].map(name=>`  ${name}:\n    image: node:24-alpine\n    command: ["node", "-e", "process.exit(0)"]\n`).join('')
    await writeFile(next,'services:\n  planner-service:\n    image: node:24-alpine\n    command: ["node", "-e", "setInterval(()=>{},1000)"]\n    labels:\n      rehearsal.version: new\n  planner-migration:\n    image: node:24-alpine\n    command: ["node", "-e", "console.log(\'INTENTIONAL_MIGRATION_FAILURE\');process.exit(7)"]\n'+otherRuntime+otherMigrations)
    docker(...compose(base),'up','-d','--no-deps','planner-service')
    const id=docker(...compose(base),'ps','-q','planner-service')
    const result=spawnSync(process.execPath,['scripts/deploy-services.mjs',env,next],{encoding:'utf8',timeout:60000})
    assert.equal(result.status,1)
    assert.match(result.stderr,/runtime containers were not replaced/)
    assert.match(result.stdout,/INTENTIONAL_MIGRATION_FAILURE/)
    assert.equal(docker(...compose(base),'ps','-q','planner-service'),id)
    assert.equal(docker('inspect','--format','{{.State.Running}} {{index .Config.Labels "rehearsal.version"}}',id),'true old')
  }finally{
    spawnSync('docker',[...compose(base),'down','--remove-orphans'],{encoding:'utf8',timeout:60000})
    await rm(dir,{recursive:true,force:true})
  }
})

import { access } from 'node:fs/promises'
import { spawnSync } from 'node:child_process'
import path from 'node:path'

const migrations=['planner-migration','activity-migration','notification-migration']
const runtime=['planner-service','activity-service','notification-service','nginx']
try {
  const [environment,...files]=process.argv.slice(2)
  if(!environment||files.length===0)throw new Error('Pass an environment file and Compose manifests')
  for(const file of [environment,...files])await access(path.resolve(file))
  const compose=['compose','--env-file',path.resolve(environment),...files.flatMap(file=>['-f',path.resolve(file)])]
  const run=(args)=>spawnSync('docker',[...compose,...args],{stdio:'inherit'}).status===0
  if(!run(['config','--quiet']))throw new Error('Compose preflight failed')
  const inventory=spawnSync('docker',[...compose,'config','--services'],{encoding:'utf8'})
  const services=new Set((inventory.stdout??'').trim().split(/\r?\n/))
  if(inventory.status!==0||[...migrations,...runtime].some(name=>!services.has(name)))throw new Error('Incomplete deployment manifest')
  for(const migration of migrations){
    if(!run(['run','--rm','--no-deps',migration])){
      console.error('Migration failed; runtime containers were not replaced.')
      process.exit(1)
    }
  }
  if(!run(['up','-d','--no-build','--no-deps','--wait','--wait-timeout','120',...runtime])){
    console.error('Runtime health check failed. Recover with the previous manifest and image digests.')
    process.exit(1)
  }
  console.log('RUNTIME_DEPLOYED_AND_HEALTHY')
} catch {
  console.error('Deployment preflight failed; runtime containers were not replaced.')
  process.exitCode=1
}

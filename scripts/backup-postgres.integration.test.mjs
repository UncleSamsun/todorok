import assert from 'node:assert/strict'
import { spawnSync } from 'node:child_process'
import { mkdtemp, writeFile, readFile, rm } from 'node:fs/promises'
import os from 'node:os'
import path from 'node:path'
import test from 'node:test'

test('an encrypted PostgreSQL archive restores all three schemas into an empty isolated database', async () => {
  const dir=await mkdtemp(path.join(os.tmpdir(),'todorok-pg-restore-'))
  const name=`todorok-restore-rehearsal-${process.pid}-${Date.now()}`
  const docker=(...args)=>{const r=spawnSync('docker',args,{encoding:'utf8',timeout:60000});assert.equal(r.status,0,r.stderr);return r.stdout.trim()}
  try {
    docker('run','-d','--name',name,'--memory=192m','-e','POSTGRES_PASSWORD=rehearsal-only','postgres:17.11-alpine')
    let ready=false
    for(let i=0;i<60;i++){
      const r=spawnSync('docker',['exec',name,'pg_isready','-U','postgres'],{encoding:'utf8',timeout:5000})
      if(r.status===0){ready=true;break}
      await new Promise(resolve=>setTimeout(resolve,500))
    }
    assert.ok(ready,'isolated PostgreSQL did not become ready')
    for(const schema of ['planner','activity','notification'])docker('exec',name,'psql','-U','postgres','-v','ON_ERROR_STOP=1','-c',`create schema ${schema}; create table ${schema}.restore_fixture(id integer primary key, detail text); insert into ${schema}.restore_fixture values (1,'${schema}-private-fixture');`)
    docker('exec',name,'pg_dump','-U','postgres','-d','postgres','-Fc','--no-owner','--no-acl','--file=/tmp/source.dump')
    docker('cp',`${name}:/tmp/source.dump`,path.join(dir,'source.dump'))
    await writeFile(path.join(dir,'key'),Buffer.alloc(32,47).toString('base64'),{mode:0o600})
    for(const [mode,input,output] of [['seal','source.dump','backup.enc'],['open','backup.enc','verified.dump']]){
      const r=spawnSync(process.execPath,['scripts/backup-archive.mjs',mode,'--input',path.join(dir,input),'--output',path.join(dir,output),'--key-file',path.join(dir,'key')],{encoding:'utf8'})
      assert.equal(r.status,0,r.stderr)
    }
    assert.deepEqual(await readFile(path.join(dir,'source.dump')),await readFile(path.join(dir,'verified.dump')))
    docker('exec',name,'createdb','-U','postgres','todorok_restore_rehearsal')
    docker('cp',path.join(dir,'verified.dump'),`${name}:/tmp/verified.dump`)
    docker('exec',name,'pg_restore','-U','postgres','-d','todorok_restore_rehearsal','--exit-on-error','--single-transaction','--no-owner','--no-acl','/tmp/verified.dump')
    for(const schema of ['planner','activity','notification'])assert.equal(docker('exec',name,'psql','-U','postgres','-d','todorok_restore_rehearsal','-Atc',`select id||':'||detail from ${schema}.restore_fixture`),`1:${schema}-private-fixture`)
  } finally {
    spawnSync('docker',['rm','-f',name],{encoding:'utf8',timeout:60000})
    await rm(dir,{recursive:true,force:true})
  }
})

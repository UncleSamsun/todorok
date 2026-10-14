import assert from 'node:assert/strict'
import { spawnSync } from 'node:child_process'
import { mkdtemp, writeFile, readFile, rm } from 'node:fs/promises'
import os from 'node:os'
import path from 'node:path'
import test from 'node:test'

test('backup archives encrypt, authenticate, and never overwrite or publish corrupt plaintext', async () => {
  const dir=await mkdtemp(path.join(os.tmpdir(),'todorok-backup-test-'))
  const run=(mode,input,output,key='key')=>spawnSync(process.execPath,['scripts/backup-archive.mjs',mode,'--input',path.join(dir,input),'--output',path.join(dir,output),'--key-file',path.join(dir,key)],{encoding:'utf8'})
  try {
    const original=Buffer.from('private-test-fixture\0'.repeat(10000))
    await writeFile(path.join(dir,'dump'),original)
    await writeFile(path.join(dir,'key'),Buffer.alloc(32,37).toString('base64'))
    await writeFile(path.join(dir,'wrong-key'),Buffer.alloc(32,38).toString('base64'))
    const sealed=run('seal','dump','backup')
    assert.equal(sealed.status,0,sealed.stderr)
    const archive=await readFile(path.join(dir,'backup'))
    assert.equal(archive.includes(Buffer.from('private-test-fixture')),false)
    assert.equal(run('seal','dump','backup').status,1)
    assert.deepEqual(await readFile(path.join(dir,'backup')),archive)
    assert.equal(run('open','backup','restored').status,0)
    assert.deepEqual(await readFile(path.join(dir,'restored')),original)
    assert.equal(run('open','backup','wrong-output','wrong-key').status,1)
    await assert.rejects(readFile(path.join(dir,'wrong-output')),{code:'ENOENT'})
    archive[archive.length-20]^=1;await writeFile(path.join(dir,'tampered'),archive)
    assert.equal(run('open','tampered','corrupt-output').status,1)
    await assert.rejects(readFile(path.join(dir,'corrupt-output')),{code:'ENOENT'})
  } finally { await rm(dir,{recursive:true,force:true}) }
})

import { createCipheriv, createDecipheriv, randomBytes, randomUUID } from 'node:crypto'
import { createReadStream, createWriteStream } from 'node:fs'
import { appendFile, link, open, readFile, unlink, writeFile } from 'node:fs/promises'
import { pipeline } from 'node:stream/promises'
import path from 'node:path'

const magic=Buffer.from('TODOROK1')
let temporary
let ownsTemporary=false
try {
  const [mode,...args]=process.argv.slice(2)
  if(!['seal','open'].includes(mode)||args.length!==6)throw new Error('Invalid arguments')
  const options={}
  for(let i=0;i<args.length;i+=2){
    if(!['--input','--output','--key-file'].includes(args[i])||options[args[i]])throw new Error('Invalid option')
    options[args[i]]=args[i+1]
  }
  if(Object.keys(options).length!==3)throw new Error('Missing option')
  const input=path.resolve(options['--input']),output=path.resolve(options['--output'])
  const encoded=(await readFile(options['--key-file'],'utf8')).trim()
  if(!/^[A-Za-z0-9+/]{43}=$/.test(encoded))throw new Error('Invalid key')
  const key=Buffer.from(encoded,'base64')
  if(key.length!==32)throw new Error('Invalid key')
  temporary=output+'.partial-'+randomUUID()
  if(mode==='seal'){
    const nonce=randomBytes(12),header=Buffer.concat([magic,nonce])
    const cipher=createCipheriv('aes-256-gcm',key,nonce);cipher.setAAD(header)
    await writeFile(temporary,header,{flag:'wx',mode:0o600});ownsTemporary=true
    await pipeline(createReadStream(input),cipher,createWriteStream(temporary,{flags:'a'}))
    await appendFile(temporary,cipher.getAuthTag())
  } else {
    const archive=await open(input,'r')
    try {
      const {size}=await archive.stat()
      if(size<36)throw new Error('Invalid archive')
      const header=Buffer.alloc(20),tag=Buffer.alloc(16)
      await archive.read(header,0,20,0);await archive.read(tag,0,16,size-16)
      if(!header.subarray(0,8).equals(magic))throw new Error('Invalid header')
      const decipher=createDecipheriv('aes-256-gcm',key,header.subarray(8));decipher.setAAD(header);decipher.setAuthTag(tag)
      await writeFile(temporary,Buffer.alloc(0),{flag:'wx',mode:0o600});ownsTemporary=true
      await pipeline(archive.createReadStream({start:20,end:size-17,autoClose:false}),decipher,createWriteStream(temporary,{flags:'a'}))
    } finally { await archive.close() }
  }
  // Publishing a hard link is atomic and fails if a previous backup/output exists.
  await link(temporary,output)
  console.log(mode==='seal'?'BACKUP_SEALED':'BACKUP_VERIFIED_AND_OPENED')
} catch {
  console.error('Backup archive operation failed. Check key, input, output, and archive integrity.')
  process.exitCode=1
} finally {
  if(ownsTemporary)await unlink(temporary).catch(()=>{})
}

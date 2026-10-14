import { generateKeyPairSync, randomBytes } from 'node:crypto'
import { mkdir, writeFile, access } from 'node:fs/promises'
import path from 'node:path'

const [directory,origin,email='mjkim@todorok.local']=process.argv.slice(2)
if(!directory||new URL(origin).protocol!=='https:')throw new Error('Provide a private directory and HTTPS origin')
const destination=path.resolve(directory)
await mkdir(destination,{recursive:true,mode:0o700})
if(await access(path.join(destination,'secret.json')).then(()=>true,()=>false))throw new Error('Existing secrets must be reused, not overwritten')
const data={}
for(const name of ['POSTGRES_PASSWORD','PLANNER_DB_PASSWORD','ACTIVITY_DB_PASSWORD','NOTIFICATION_DB_PASSWORD','DEBEZIUM_DB_PASSWORD','TODOROK_BOOTSTRAP_PASSWORD'])data[name]=randomBytes(32).toString('base64url')
data.TODOROK_BOOTSTRAP_EMAIL=email
for(const name of ['auth','template']){
  const pair=generateKeyPairSync('rsa',{modulusLength:2048})
  data[name+'-private.pem']=pair.privateKey.export({type:'pkcs8',format:'pem'})
  data[name+'-public.pem']=pair.publicKey.export({type:'spki',format:'pem'})
}
const ec=generateKeyPairSync('ec',{namedCurve:'prime256v1'}).privateKey.export({format:'jwk'})
data['push-public.key']=Buffer.concat([Buffer.from([4]),Buffer.from(ec.x,'base64url'),Buffer.from(ec.y,'base64url')]).toString('base64url')
data['push-private.key']=ec.d
data['push-encryption.key']=randomBytes(32).toString('base64')
await writeFile(path.join(destination,'secret.json'),JSON.stringify({apiVersion:'v1',kind:'Secret',metadata:{name:'todorok-secrets',namespace:'todorok-test',labels:{'app.kubernetes.io/part-of':'todorok'}},type:'Opaque',data:Object.fromEntries(Object.entries(data).map(([key,value])=>[key,Buffer.from(value).toString('base64')]))},null,2),{flag:'wx',mode:0o600})
await writeFile(path.join(destination,'access.json'),JSON.stringify({url:origin+'/todorok/',email,password:data.TODOROK_BOOTSTRAP_PASSWORD},null,2),{flag:'wx',mode:0o600})
console.log('Generated dedicated secrets and local access file; values were not printed.')

import assert from 'node:assert/strict'
import { access } from 'node:fs/promises'
import test from 'node:test'

test('company manifests isolate storage/network and pin application images without publishing secrets',async()=>{
  assert.ok(await access('scripts/render-company-deployment.mjs').then(()=>true,()=>false),'Company deployment renderer is not implemented')
  const { renderCompanyDeployment }=await import('./render-company-deployment.mjs')
  const images=Object.fromEntries(['planner','activity','notification','web','connect-init'].map(name=>[name,`ghcr.io/unclesamsun/todorok-${name}@sha256:${'a'.repeat(64)}`]))
  const result=await renderCompanyDeployment({images,origin:'https://server.example',release:'abc123def456',dns:'10.43.0.10'})
  const all=Object.values(result).flat()
  assert.equal(all.filter(x=>x.kind==='Namespace')[0].metadata.name,'todorok-test')
  assert.equal(all.some(x=>x.kind==='Secret'),false)
  for(const resource of all.filter(x=>x.kind!=='Namespace'))assert.equal(resource.metadata.namespace,'todorok-test')
  for(const resource of all.filter(x=>['StatefulSet','Deployment','Job'].includes(x.kind))){
    const pod=resource.spec.template.spec
    assert.equal(pod.automountServiceAccountToken,false)
    assert.equal(pod.hostNetwork,undefined)
    assert.ok(pod.containers.every(c=>c.ports?.every(p=>!p.hostPort)??true))
  }
  assert.ok(all.filter(x=>x.kind==='Service').every(x=>x.spec.type==='ClusterIP'))
  assert.ok(all.some(x=>x.kind==='NetworkPolicy'))
  const nginx=result.foundation.find(x=>x.metadata.name==='todorok-nginx').data['nginx.conf']
  assert.ok(nginx.includes('notification-service.todorok-test.svc.cluster.local:8083 resolve'))
  assert.equal(result.runtime.filter(x=>x.kind==='Deployment').length,4)
  assert.ok(result.edge.find(x=>x.kind==='IngressRoute').spec.routes.some(x=>x.match.includes('/todorok/')))
  assert.ok(result.jobs.some(x=>x.metadata.name.startsWith('planner-migration-')))
  await assert.rejects(renderCompanyDeployment({images:{...images,planner:'ghcr.io/unclesamsun/todorok-planner:latest'},origin:'https://server.example',release:'abc123',dns:'10.43.0.10'}),/digest/)
})

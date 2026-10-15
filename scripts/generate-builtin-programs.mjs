import {mkdir,writeFile} from 'node:fs/promises'
import {createHash} from 'node:crypto'
import path from 'node:path'
import {fileURLToPath} from 'node:url'
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..')

// Numeric workout targets only. Provenance is kept outside the user interface.
// Hundred Pushups: week1.html through week6.html, checked 2026-10-15.
const pushups=[
  [[[2,3,2,2,3],[6,6,4,4,5],[10,12,7,7,9]],[[3,4,2,3,4],[6,8,6,6,7],[10,12,8,8,12]],[[4,5,4,4,5],[8,10,7,7,10],[11,15,9,9,13]]],
  [[[4,6,4,4,6],[9,11,8,8,11],[14,14,10,10,15]],[[5,6,4,4,7],[10,12,9,9,13],[14,16,12,12,17]],[[5,7,5,5,8],[12,13,10,10,15],[16,17,14,14,20]]],
  [[[10,12,7,7,9],[12,17,13,13,17],[14,18,14,14,20]],[[10,12,8,8,12],[14,19,14,14,19],[20,25,15,15,25]],[[11,13,9,9,13],[16,21,15,15,21],[22,30,20,20,28]]],
  [[[12,14,11,10,16],[18,22,16,16,25],[21,25,21,21,32]],[[14,16,12,12,18],[20,25,20,20,28],[25,29,25,25,36]],[[16,18,13,13,20],[23,28,23,23,33],[29,33,29,29,40]]],
  [[[17,19,15,15,20],[28,35,25,22,35],[36,40,30,24,40]],[[10,10,13,13,10,10,9,25],[18,18,20,20,14,14,16,40],[19,19,22,22,18,18,22,45]],[[13,13,15,15,12,12,10,30],[18,18,20,20,17,17,20,45],[20,20,24,24,20,20,22,50]]],
  [[[25,30,20,15,40],[40,50,25,25,50],[45,55,35,30,55]],[[14,14,15,15,14,14,10,10,44],[20,20,23,23,20,20,18,18,53],[22,22,30,30,24,24,18,18,58]],[[13,13,17,17,16,16,14,14,50],[22,22,30,30,25,25,18,18,55],[26,26,33,33,26,26,22,22,60]]],
]
// Recon Ron: 38 weekly targets, independently checked against the published table.
const pullupColumns=[
  [6,7,8,8,9,10,10,11,12,12,13,14,14,15,16,16,17,18,18,19,20,20,21,22,22,23,24,24,25,26,26,27,28,28,29,30,30,31],
  [5,6,6,7,7,7,8,8,8,9,9,9,10,10,10,11,11,11,12,12,12,13,13,13,14,14,14,15,15,15,16,16,16,17,17,17,18,18],
  [5,5,5,5,6,6,6,7,7,7,8,8,8,9,9,9,10,10,10,11,11,11,12,12,12,13,13,13,14,14,14,15,15,15,16,16,16,17],
  [4,4,5,5,5,6,6,6,7,7,7,8,8,8,9,9,9,10,10,10,11,11,11,12,12,12,13,13,13,14,14,14,15,15,15,16,16,16],
  [3,4,4,5,5,5,6,6,6,7,7,7,8,8,8,9,9,9,10,10,10,11,11,11,12,12,12,13,13,13,14,14,14,15,15,15,16,16],
]
const pullups=Array.from({length:38},(_,index)=>pullupColumns.map(column=>column[index]))
for(const [index,sets]of pullups.entries())if(sets.reduce((sum,n)=>sum+n,0)!==(index===0?23:24+index*2))throw Error('Pullup total mismatch at week '+(index+1))
const presets={
  'hundred-pushups':{name:'푸시업',weeks:pushups,restSeconds:pushups.map((_,week)=>week===0?[60,60,60]:week>=4?[60,45,45]:[60,90,120]),lastSetMinimum:true,sources:Array.from({length:6},(_,i)=>`https://hundredpushups.com/week${i+1}.html`)},
  'recon-ron-pullups':{name:'풀업',weeks:pullups.map(sets=>Array.from({length:3},()=>[sets])),restSeconds:pullups.map(()=>[60,60,60]),lastSetMinimum:false,sources:['https://www.scribd.com/document/817585441/Recon-Ron-Pullup-Program','https://demand-and-supply.tistory.com/11?category=870378']},
}
const canonical=value=>Array.isArray(value)?'['+value.map(canonical).join(',')+']':value&&typeof value==='object'?'{'+Object.keys(value).filter(key=>key!=='checksum').sort().map(key=>JSON.stringify(key)+':'+canonical(value[key])).join(',')+'}':JSON.stringify(value)
const catalogs=[]
for(const [key,preset]of Object.entries(presets))for(let level=0;level<(key==='hundred-pushups'?3:1);level++){
  const catalog={catalogKey:key==='hundred-pushups'?`${key}-level-${level+1}`:key,version:1,name:preset.name,source:{kind:'SOURCE_BASED',label:key==='hundred-pushups'?'Hundred Pushups':'Recon Ron Pull-Up Program',url:preset.sources[0],conditions:[key==='hundred-pushups'?'해당 난이도의 최소 횟수를 기준으로 수행':'정자세 풀업 10회 이상 가능'],cautions:['통증 발생 시 운동 중단']},sessionsPerWeek:3,weeks:preset.weeks.map((sessions,i)=>({week:i+1,sessions:sessions.map((levels,j)=>{const sets=levels[level];return {session:j+1,sets,targetTotal:sets.reduce((sum,n)=>sum+n,0)}})}))}
  catalogs.push({...catalog,checksum:createHash('sha256').update(canonical(catalog)).digest('hex')})
}
const dataDir=path.join(root,'contracts/catalog/builtin')
const resourceDir=path.join(root,'services/activity-service/src/main/resources/catalog/builtin')
await mkdir(dataDir,{recursive:true});await mkdir(resourceDir,{recursive:true})
await writeFile(path.join(dataDir,'presets.json'),JSON.stringify(presets,null,2)+'\n')
for(const catalog of catalogs)await writeFile(path.join(resourceDir,catalog.catalogKey+'.json'),JSON.stringify(catalog,null,2)+'\n')
await writeFile(path.join(dataDir,'catalogs.json'),JSON.stringify(catalogs,null,2)+'\n')
console.log('Generated 6-week / 3-level pushups and 38-week pullups with immutable checksums.')

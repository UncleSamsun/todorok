import { readFile, mkdir, writeFile } from 'node:fs/promises'
import path from 'node:path'
import { pathToFileURL } from 'node:url'

export async function renderCompanyDeployment({images,origin,release,dns,aliases=[]}){
  const namespace='todorok-test'
  for(const name of ['planner','activity','notification','web','connect-init'])
    if(!new RegExp(`^ghcr.io/unclesamsun/todorok-${name}@sha256:[a-f0-9]{64}$`).test(images?.[name]??''))throw new Error(`Missing immutable image digest: ${name}`)
  const url=new URL(origin)
  if(url.protocol!=='https:'||url.origin!==origin)throw new Error('An HTTPS origin without a path is required')
  const origins=[...new Set([origin,...aliases])]
  for(const address of origins){const candidate=new URL(address);if(candidate.protocol!=='https:'||candidate.origin!==address)throw new Error('An HTTPS origin without a path is required')}
  const serverIps=origins.map(address=>new URL(address).hostname).filter(host=>/^\d+\.\d+\.\d+\.\d+$/.test(host)).map(host=>host+'/32')
  if(!/^[a-z0-9-]{6,32}$/.test(release))throw new Error('Invalid release identifier')
  if(!/^\d{1,3}(\.\d{1,3}){3}$/.test(dns))throw new Error('A cluster DNS IPv4 address is required')
  const labels=name=>({'app.kubernetes.io/name':name,'app.kubernetes.io/part-of':'todorok'})
  const resource=(apiVersion,kind,name,spec,extra={})=>({apiVersion,kind,metadata:{name,namespace,labels:labels(name)},...(spec?{spec}:{}),...extra})
  const secret=(name,key=name)=>({name,valueFrom:{secretKeyRef:{name:'todorok-secrets',key}}})
  const value=(name,value)=>({name,value:String(value)})
  const dbEnv=user=>[value('SPRING_DATASOURCE_URL','jdbc:postgresql://postgres:5432/todorok?currentSchema='+user),value('SPRING_DATASOURCE_USERNAME',user+'_app'),secret('SPRING_DATASOURCE_PASSWORD',user.toUpperCase()+'_DB_PASSWORD')]
  const adminEnv=[value('POSTGRES_DB','todorok'),value('POSTGRES_USER','postgres'),secret('POSTGRES_PASSWORD'),secret('PLANNER_DB_PASSWORD'),secret('ACTIVITY_DB_PASSWORD'),secret('NOTIFICATION_DB_PASSWORD'),secret('DEBEZIUM_DB_PASSWORD')]
  const scriptsVolume={name:'setup',configMap:{name:'todorok-setup'}}
  const keysVolume={name:'keys',secret:{secretName:'todorok-secrets',items:['auth-public.pem','auth-private.pem','template-public.pem','template-private.pem','push-public.key','push-private.key','push-encryption.key'].map(key=>({key,path:key}))}}
  const setupMount={name:'setup',mountPath:'/setup',readOnly:true}
  const keysMount={name:'keys',mountPath:'/run/secrets',readOnly:true}
  const resources=(memory,cpu='1000m')=>({requests:{cpu:'100m',memory:memory==='64Mi'?'32Mi':'128Mi'},limits:{cpu,memory}})
  const pod=(name,container,volumes=[])=>({metadata:{labels:labels(name)},spec:{automountServiceAccountToken:false,containers:[{name,...container}],...(volumes.length?{volumes}:{})}})
  const service=(name,port)=>resource('v1','Service',name,{type:'ClusterIP',selector:labels(name),ports:[{name:'http',port,targetPort:port}]})
  const deployment=(name,container,volumes=[])=>resource('apps/v1','Deployment',name,{replicas:1,strategy:{type:'Recreate'},selector:{matchLabels:labels(name)},template:pod(name,container,volumes)})
  const job=(name,container,volumes=[])=>{
    const [host,port]=name==='kafka-init'?['kafka',19092]:name==='connect-init'?['connect',8083]:['postgres',5432]
    return resource('batch/v1','Job',name+'-'+release,{backoffLimit:1,activeDeadlineSeconds:600,ttlSecondsAfterFinished:86400,template:{...pod(name,container,volumes),spec:{...pod(name,container,volumes).spec,restartPolicy:'Never',initContainers:[{
      name:'wait-dependency',image:'busybox:1.37.0',command:['sh','-ec',`for attempt in $(seq 1 90); do nc -z -w 2 ${host} ${port} && exit 0; sleep 2; done; exit 1`],resources:{requests:{cpu:'10m',memory:'16Mi'},limits:{cpu:'100m',memory:'32Mi'}},
    }]}}})
  }
  const httpProbe=(port,route)=>({httpGet:{port,path:route},periodSeconds:10,timeoutSeconds:5,failureThreshold:30})
  const script=async file=>(await readFile(file,'utf8')).replaceAll('\r\n','\n')
  const nginx=(await script('infra/nginx/nginx.conf'))
    .replace('resolver 127.0.0.11',`resolver ${dns}`)
    .replace('notification-service:8083 resolve','notification-service.todorok-test.svc.cluster.local:8083 resolve')
  const foundation=[{apiVersion:'v1',kind:'Namespace',metadata:{name:namespace,labels:{'app.kubernetes.io/part-of':'todorok'}}},
    resource('v1','ResourceQuota','todorok-budget',{hard:{'limits.memory':'8Gi','limits.cpu':'8','requests.storage':'40Gi',persistentvolumeclaims:'4'}}),
    resource('v1','ConfigMap','todorok-setup',null,{data:{
      'create-roles.sh':await script('infra/docker/postgres/init/001-create-service-roles.sh'),
      'replication.sh':await script('infra/docker/postgres/replication/initialize-outbox-replication.sh'),
      'initialize-topics.sh':await script('infra/docker/kafka/initialize-topics.sh'),
    }}),resource('v1','ConfigMap','todorok-nginx',null,{data:{'nginx.conf':nginx}}),
    resource('networking.k8s.io/v1','NetworkPolicy','todorok-isolation',{
      podSelector:{},policyTypes:['Ingress','Egress'],
      ingress:[{from:[{podSelector:{}}]},{from:[{namespaceSelector:{matchLabels:{'kubernetes.io/metadata.name':'kube-system'}},podSelector:{matchLabels:{'app.kubernetes.io/name':'traefik'}}}],ports:[{protocol:'TCP',port:80}]}],
      egress:[{to:[{podSelector:{}}]},{to:[{namespaceSelector:{matchLabels:{'kubernetes.io/metadata.name':'kube-system'}},podSelector:{matchLabels:{'k8s-app':'kube-dns'}}}],ports:[{port:53,protocol:'UDP'},{port:53,protocol:'TCP'}]},
        {to:[{ipBlock:{cidr:'0.0.0.0/0',except:['10.0.0.0/8','172.16.0.0/12','192.168.0.0/16','127.0.0.0/8','169.254.0.0/16',...serverIps]}}],ports:[{port:443,protocol:'TCP'}]}],
    })]
  const postgres=resource('apps/v1','StatefulSet','postgres',{serviceName:'postgres',replicas:1,selector:{matchLabels:labels('postgres')},
    template:pod('postgres',{image:'postgres:17.11-alpine',args:['postgres','-c','wal_level=logical','-c','max_wal_senders=1','-c','max_replication_slots=1','-c','max_slot_wal_keep_size=2048MB'],env:adminEnv,
      resources:resources('512Mi','500m'),ports:[{containerPort:5432}],volumeMounts:[{name:'data',mountPath:'/var/lib/postgresql/data'}],
      readinessProbe:{exec:{command:['sh','-c','pg_isready -U postgres -d todorok']},periodSeconds:5},livenessProbe:{exec:{command:['sh','-c','pg_isready -U postgres -d todorok']},initialDelaySeconds:30,periodSeconds:20}}),
    volumeClaimTemplates:[{metadata:{name:'data'},spec:{accessModes:['ReadWriteOnce'],resources:{requests:{storage:'10Gi'}}}}]})
  const kafkaEnv={KAFKA_NODE_ID:1,KAFKA_PROCESS_ROLES:'broker,controller',KAFKA_LISTENERS:'CONTROLLER://:9093,PLAINTEXT://:19092',KAFKA_ADVERTISED_LISTENERS:'PLAINTEXT://kafka:19092',KAFKA_LISTENER_SECURITY_PROTOCOL_MAP:'CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT',KAFKA_CONTROLLER_LISTENER_NAMES:'CONTROLLER',KAFKA_INTER_BROKER_LISTENER_NAME:'PLAINTEXT',KAFKA_CONTROLLER_QUORUM_VOTERS:'1@127.0.0.1:9093',KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR:1,KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR:1,KAFKA_TRANSACTION_STATE_LOG_MIN_ISR:1,KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS:0,KAFKA_LOG_DIRS:'/var/lib/kafka/data/logs',KAFKA_HEAP_OPTS:'-Xms256m -Xmx384m'}
  const kafka=resource('apps/v1','StatefulSet','kafka',{serviceName:'kafka',replicas:1,selector:{matchLabels:labels('kafka')},
    template:pod('kafka',{image:'apache/kafka:4.3.1',env:Object.entries(kafkaEnv).map(([k,v])=>value(k,v)),resources:resources('768Mi'),ports:[{containerPort:19092},{containerPort:9093}],volumeMounts:[{name:'data',mountPath:'/var/lib/kafka/data'}],readinessProbe:{tcpSocket:{port:19092},initialDelaySeconds:15,periodSeconds:5}}),
    volumeClaimTemplates:[{metadata:{name:'data'},spec:{accessModes:['ReadWriteOnce'],resources:{requests:{storage:'10Gi'}}}}]})
  kafka.spec.template.spec.securityContext={fsGroup:1000}
  const infra=[service('postgres',5432),service('kafka',19092),postgres,kafka]
  const connectEnv={BOOTSTRAP_SERVERS:'kafka:19092',GROUP_ID:'todorok-connect',CONFIG_STORAGE_TOPIC:'todorok.connect.configs',OFFSET_STORAGE_TOPIC:'todorok.connect.offsets',STATUS_STORAGE_TOPIC:'todorok.connect.status',CONFIG_STORAGE_REPLICATION_FACTOR:1,OFFSET_STORAGE_REPLICATION_FACTOR:1,STATUS_STORAGE_REPLICATION_FACTOR:1,KEY_CONVERTER:'org.apache.kafka.connect.storage.StringConverter',VALUE_CONVERTER:'org.apache.kafka.connect.json.JsonConverter',VALUE_CONVERTER_SCHEMAS_ENABLE:false,KAFKA_HEAP_OPTS:'-Xms256m -Xmx384m'}
  const connect=[service('connect',8083),deployment('connect',{image:'quay.io/debezium/connect:3.6.2.Final',env:Object.entries(connectEnv).map(([k,v])=>value(k,v)),resources:resources('768Mi'),ports:[{containerPort:8083}],startupProbe:httpProbe(8083,'/connectors'),readinessProbe:httpProbe(8083,'/connectors')})]
  const jobs=[job('postgres-provision',{image:'postgres:17.11-alpine',command:['bash','/setup/create-roles.sh'],env:[...adminEnv,value('POSTGRES_HOST','postgres')],resources:resources('128Mi'),volumeMounts:[setupMount]},[scriptsVolume]),
    ...['planner','activity','notification'].map(name=>job(name+'-migration',{image:images[name],command:['java','-jar','/app/migration.jar','--spring.profiles.active=migration'],env:[...dbEnv(name),value('JAVA_TOOL_OPTIONS','-XX:MaxRAMPercentage=60')],resources:resources('512Mi')})),
    job('kafka-init',{image:'apache/kafka:4.3.1',command:['bash','/setup/initialize-topics.sh'],env:[value('KAFKA_HEAP_OPTS','-Xms64m -Xmx128m')],resources:resources('256Mi'),volumeMounts:[setupMount]},[scriptsVolume]),
    job('replication-init',{image:'postgres:17.11-alpine',command:['bash','/setup/replication.sh'],env:adminEnv,resources:resources('128Mi'),volumeMounts:[setupMount]},[scriptsVolume]),
    job('connect-init',{image:images['connect-init'],env:[value('CONNECT_URL','http://connect:8083'),value('POSTGRES_DB','todorok'),secret('DEBEZIUM_DB_PASSWORD')],resources:resources('128Mi')}),
    job('planner-bootstrap',{image:images.planner,command:['java','-jar','/app/bootstrap.jar'],env:[...dbEnv('planner'),secret('TODOROK_BOOTSTRAP_EMAIL'),secret('TODOROK_BOOTSTRAP_PASSWORD')],resources:resources('512Mi')})]
  const runtime=[]
  for(const [name,port,memory] of [['planner',8081,'512Mi'],['activity',8082,'640Mi'],['notification',8083,'384Mi']]){
    const env=[...dbEnv(name),value('MESSAGING_ENABLED','true'),value('SPRING_KAFKA_BOOTSTRAP_SERVERS','kafka:19092'),value('SPRING_FLYWAY_ENABLED','false'),value('AUTH_PUBLIC_KEY_LOCATION','file:/run/secrets/auth-public.pem'),value('JAVA_TOOL_OPTIONS','-XX:InitialRAMPercentage=20 -XX:MaxRAMPercentage=60 -XX:+ExitOnOutOfMemoryError')]
    if(name==='planner')env.push(value('AUTH_PRIVATE_KEY_LOCATION','file:/run/secrets/auth-private.pem'),value('AUTH_ALLOWED_ORIGINS',origins.join(',')),value('AUTH_COOKIE_SECURE','true'),value('TODOROK_TEMPLATE_SERVICE_PRIVATE_KEY_FILE','file:/run/secrets/template-private.pem'))
    if(name==='activity')env.push(value('TODOROK_TEMPLATE_SERVICE_PUBLIC_KEY_FILE','file:/run/secrets/template-public.pem'))
    if(name==='notification')env.push(value('PUSH_ENABLED','true'),value('PUSH_WORKER_ENABLED','true'),value('PUSH_PUBLIC_KEY_LOCATION','file:/run/secrets/push-public.key'),value('PUSH_PRIVATE_KEY_LOCATION','file:/run/secrets/push-private.key'),value('PUSH_ENCRYPTION_KEY_LOCATION','file:/run/secrets/push-encryption.key'),value('PUSH_SUBJECT',origin+'/todorok/'))
    const health=name==='notification'?'/actuator/health':`/api/${name}/v1/actuator/health`
    // Mount only the keys each service uses, rather than every account secret.
    const required=name==='planner'?['auth-public.pem','auth-private.pem','template-private.pem']:name==='activity'?['auth-public.pem','template-public.pem']:['auth-public.pem','push-public.key','push-private.key','push-encryption.key']
    runtime.push(service(name+'-service',port),deployment(name+'-service',{image:images[name],env,resources:resources(memory),ports:[{containerPort:port}],volumeMounts:[keysMount],startupProbe:httpProbe(port,health),readinessProbe:httpProbe(port,health),livenessProbe:{...httpProbe(port,health),failureThreshold:6}},[{...keysVolume,secret:{...keysVolume.secret,items:required.map(key=>({key,path:key}))}}]))
  }
  runtime.push(service('web',80),deployment('web',{image:images.web,resources:resources('64Mi','200m'),ports:[{containerPort:80}],volumeMounts:[{name:'nginx',mountPath:'/etc/nginx/nginx.conf',subPath:'nginx.conf',readOnly:true}],readinessProbe:httpProbe(80,'/health')},[{name:'nginx',configMap:{name:'todorok-nginx'}}]))
  const host='('+origins.map(address=>'Host(`'+new URL(address).hostname+'`)').join(' || ')+')'
  const edge=[resource('traefik.io/v1alpha1','Middleware','todorok-slash',{redirectRegex:{regex:'^(https?://[^/]+/todorok)$',replacement:'${1}/',permanent:true}}),
    resource('traefik.io/v1alpha1','Middleware','todorok-strip',{stripPrefix:{prefixes:['/todorok']}}),
    resource('traefik.io/v1alpha1','IngressRoute','todorok-public',{entryPoints:['websecure'],routes:[
      {kind:'Rule',match:host+' && (Path(`/todorok`) || PathPrefix(`/todorok/`))',priority:200,middlewares:[{name:'todorok-slash'},{name:'todorok-strip'}],services:[{name:'web',port:80}]},
      {kind:'Rule',match:host+' && (PathPrefix(`/api/planner/`) || PathPrefix(`/api/activity/`) || PathPrefix(`/api/notification/`))',priority:200,services:[{name:'web',port:80}]},
    ],tls:{}})]
  return {foundation,infra,connect,jobs,runtime,edge}
}

if(import.meta.url===pathToFileURL(process.argv[1]).href){
  const [imagesFile,output,origin,release,dns,aliases='']=process.argv.slice(2)
  const phases=await renderCompanyDeployment({images:JSON.parse(await readFile(imagesFile,'utf8')),origin,release,dns,aliases:aliases?aliases.split(','):[]})
  await mkdir(output,{recursive:true})
  for(const [name,items] of Object.entries(phases))await writeFile(path.join(output,name+'.json'),JSON.stringify({apiVersion:'v1',kind:'List',items},null,2)+'\n')
  console.log('Deployment phases rendered without secrets.')
}

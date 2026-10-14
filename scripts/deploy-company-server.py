"""Apply an already reviewed release to the dedicated namespace, phase by phase."""
import json
import pathlib
import subprocess
import sys

directory = pathlib.Path(sys.argv[1]).resolve()
namespace = 'todorok-test'

def kube(*args, capture=False):
    result = subprocess.run(['kubectl', *args], text=True, stdout=subprocess.PIPE if capture else None, check=True)
    return result.stdout if capture else None

def apply_phase(name, items=None):
    file = directory / (name + '.json')
    if items is not None:
        file.write_text(json.dumps({'apiVersion': 'v1', 'kind': 'List', 'items': items}))
    kube('apply', '-f', str(file))

current = json.loads(kube('get', 'namespace', namespace, '-o', 'json', capture=True))
if current['metadata'].get('labels', {}).get('app.kubernetes.io/part-of') != 'todorok':
    raise SystemExit('Refusing to use an unrelated namespace')

apply_phase('foundation')
kube('apply', '-f', str(directory / 'secret.json'))
apply_phase('infra')
for name in ['postgres', 'kafka']:
    kube('-n', namespace, 'rollout', 'status', 'statefulset/' + name, '--timeout=300s')
    claim = json.loads(kube('-n', namespace, 'get', 'pvc', 'data-' + name + '-0', '-o', 'json', capture=True))
    pv = claim['spec']['volumeName']
    volume = json.loads(kube('get', 'pv', pv, '-o', 'json', capture=True))
    if volume['spec']['claimRef']['namespace'] != namespace:
        raise SystemExit('Unexpected persistent volume ownership')
    kube('patch', 'pv', pv, '--type=merge', '-p', json.dumps({'spec': {'persistentVolumeReclaimPolicy': 'Retain'}}))

jobs = json.loads((directory / 'jobs.json').read_text())['items']
def run_job(prefix):
    job = next(item for item in jobs if item['metadata']['name'].startswith(prefix + '-'))
    apply_phase('job-' + prefix, [job])
    kube('-n', namespace, 'wait', '--for=condition=complete', 'job/' + job['metadata']['name'], '--timeout=600s')

run_job('postgres-provision')
for name in ['planner', 'activity', 'notification']:
    run_job(name + '-migration')
run_job('kafka-init')
run_job('replication-init')
apply_phase('connect')
kube('-n', namespace, 'rollout', 'status', 'deployment/connect', '--timeout=300s')
run_job('connect-init')
run_job('planner-bootstrap')
apply_phase('runtime')
for name in ['planner-service', 'activity-service', 'notification-service', 'web']:
    kube('-n', namespace, 'rollout', 'status', 'deployment/' + name, '--timeout=300s')
apply_phase('edge')
print('TODOROK_TEST_RELEASE_READY')

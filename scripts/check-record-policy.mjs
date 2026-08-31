import { spawnSync } from 'node:child_process'

const rawTerms = process.env.RECORD_POLICY_FORBIDDEN_TERMS
if (!rawTerms?.trim()) {
  throw new Error('RECORD_POLICY_FORBIDDEN_TERMS 환경 변수가 필요합니다.')
}

const terms = rawTerms
  .split(',')
  .map((term) => term.trim())
  .filter(Boolean)

if (terms.length === 0) {
  throw new Error('검사할 기록 정책 문자열이 하나 이상 필요합니다.')
}

const escapePattern = (value) => value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
const pattern = terms.map(escapePattern).join('|')

const trackedFiles = spawnSync(
  'git',
  ['grep', '-n', '-I', '-i', '-E', pattern, 'HEAD'],
  { encoding: 'utf8' },
)

if (trackedFiles.status === 0) {
  process.stderr.write(trackedFiles.stdout)
  throw new Error('저장소 파일에서 기록 정책 위반을 발견했습니다.')
}
if (trackedFiles.status !== 1) {
  process.stderr.write(trackedFiles.stderr)
  throw new Error('저장소 파일 검사를 실행하지 못했습니다.')
}

const log = spawnSync('git', ['log', '--format=%B%x00'], { encoding: 'utf8' })
if (log.status !== 0) {
  process.stderr.write(log.stderr)
  throw new Error('커밋 기록을 읽지 못했습니다.')
}

const violation = new RegExp(pattern, 'i')
if (violation.test(log.stdout)) {
  throw new Error('커밋 기록에서 기록 정책 위반을 발견했습니다.')
}

console.log('저장소 파일과 커밋 기록 정책 검사를 통과했습니다.')

import assert from 'node:assert/strict'
import test from 'node:test'
import { readFile } from 'node:fs/promises'

test('없는 hashed asset은 SPA index가 아니라 404로 끝난다', async () => {
  const nginx = await readFile('infra/nginx/nginx.conf', 'utf8')
  assert.match(nginx, /location \^~ \/assets\/\s*\{\s*try_files \$uri =404;/s)
})

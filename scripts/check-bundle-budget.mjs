import { gzipSync } from 'node:zlib'
import { readdir, readFile } from 'node:fs/promises'
import path from 'node:path'

const assets = path.resolve('apps/web/dist/assets')
const entries = await readdir(assets)
const javascript = entries.filter((name) => name.endsWith('.js'))
const initial = javascript.filter((name) => /^index-[\w-]+\.js$/.test(name))
if (initial.length !== 1) throw new Error(`Expected one initial JS bundle, found ${initial.length}`)
const size = async (name) => gzipSync(await readFile(path.join(assets, name))).length
const initialBytes = await size(initial[0])
const lazy = await Promise.all(javascript.filter((name) => name !== initial[0]).map(async (name) => [name, await size(name)]))
if (initialBytes > 150 * 1024) throw new Error(`Initial gzip bundle exceeds 150KB: ${initialBytes}`)
for (const [name, bytes] of lazy) if (bytes > 100 * 1024) throw new Error(`Lazy gzip bundle exceeds 100KB: ${name} ${bytes}`)
console.log(`BUNDLE_BUDGET_PASS initial=${initialBytes} lazy=${lazy.length}`)

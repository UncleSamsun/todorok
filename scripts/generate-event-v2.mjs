import { readFile, mkdir, writeFile } from 'node:fs/promises'
import path from 'node:path'

// The closed JSON schemas are the source of truth; Java records carry exactly their payload members.
const root = path.resolve(process.argv[2] ?? '.')
const output = path.join(root, 'libs/event-contracts/src/generated/java/io/todorok/contracts/events/v2')
await mkdir(output, { recursive: true })
for (const source of ['template-binding-link/v1', 'task-scheduled/v2', 'task-changed/v2', 'task-rolled-over/v2', 'series-changed/v2']) {
  const schema = JSON.parse(await readFile(`contracts/events/${source}.schema.json`, 'utf8'))
  const payload = schema.properties ? schema : schema.allOf[1].properties.payload
  const members = Object.entries(payload.properties).map(([name, shape]) => {
    const type = shape.anyOf ? 'TemplateBindingLink' : shape.format === 'uuid' ? 'java.util.UUID'
      : shape.format === 'date' ? 'java.time.LocalDate' : shape.type === 'integer' ? 'long' : 'String'
    return `    ${type} ${name}`
  })
  const java = `package io.todorok.contracts.events.v2;\n\n// Generated from contracts/events/${source}.schema.json.\npublic record ${schema['x-java-name']}(\n${members.join(',\n')}\n) {}\n`
  await writeFile(path.join(output, `${schema['x-java-name']}.java`), java)
}

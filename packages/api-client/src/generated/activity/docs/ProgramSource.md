
# ProgramSource


## Properties

Name | Type
------------ | -------------
`kind` | string
`label` | string
`url` | string
`conditions` | Array&lt;string&gt;
`cautions` | Array&lt;string&gt;

## Example

```typescript
import type { ProgramSource } from '@todorok/api-client'

// TODO: Update the object below with actual values
const example = {
  "kind": null,
  "label": null,
  "url": null,
  "conditions": null,
  "cautions": null,
} satisfies ProgramSource

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ProgramSource
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

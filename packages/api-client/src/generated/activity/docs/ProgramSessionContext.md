
# ProgramSessionContext


## Properties

Name | Type
------------ | -------------
`week` | number
`session` | number
`sessionsPerWeek` | number
`canRepeatWeek` | boolean

## Example

```typescript
import type { ProgramSessionContext } from '@todorok/api-client'

// TODO: Update the object below with actual values
const example = {
  "week": null,
  "session": null,
  "sessionsPerWeek": null,
  "canRepeatWeek": null,
} satisfies ProgramSessionContext

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ProgramSessionContext
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

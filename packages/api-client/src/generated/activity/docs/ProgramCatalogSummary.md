
# ProgramCatalogSummary


## Properties

Name | Type
------------ | -------------
`catalogKey` | string
`catalogVersion` | number
`checksum` | string
`name` | string
`sessionsPerWeek` | number
`totalWeeks` | number

## Example

```typescript
import type { ProgramCatalogSummary } from '@todorok/api-client'

// TODO: Update the object below with actual values
const example = {
  "catalogKey": null,
  "catalogVersion": null,
  "checksum": null,
  "name": null,
  "sessionsPerWeek": null,
  "totalWeeks": null,
} satisfies ProgramCatalogSummary

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ProgramCatalogSummary
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

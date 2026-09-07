
# LegacyStudyPayload

Response-only original JSONB. Never interpret as validated template definitions or submit as input.

## Properties

Name | Type
------------ | -------------
`provenance` | string
`values` | any
`snapshot` | any

## Example

```typescript
import type { LegacyStudyPayload } from '@todorok/api-client'

// TODO: Update the object below with actual values
const example = {
  "provenance": null,
  "values": null,
  "snapshot": null,
} satisfies LegacyStudyPayload

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as LegacyStudyPayload
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

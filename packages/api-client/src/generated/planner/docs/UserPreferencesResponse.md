
# UserPreferencesResponse


## Properties

Name | Type
------------ | -------------
`theme` | [ThemeMode](ThemeMode.md)
`notificationsEnabled` | boolean
`summaryTime` | string
`revision` | number

## Example

```typescript
import type { UserPreferencesResponse } from '@todorok/api-client'

// TODO: Update the object below with actual values
const example = {
  "theme": null,
  "notificationsEnabled": null,
  "summaryTime": null,
  "revision": null,
} satisfies UserPreferencesResponse

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as UserPreferencesResponse
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

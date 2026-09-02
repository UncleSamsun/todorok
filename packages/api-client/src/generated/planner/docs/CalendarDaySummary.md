
# CalendarDaySummary


## Properties

Name | Type
------------ | -------------
`date` | Date
`totalCount` | number
`completedCount` | number

## Example

```typescript
import type { CalendarDaySummary } from '@todorok/api-client'

// TODO: Update the object below with actual values
const example = {
  "date": null,
  "totalCount": null,
  "completedCount": null,
} satisfies CalendarDaySummary

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as CalendarDaySummary
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)



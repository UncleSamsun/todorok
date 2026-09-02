
# CalendarSummaryResponse


## Properties

Name | Type
------------ | -------------
`from` | Date
`to` | Date
`days` | [Array&lt;CalendarDaySummary&gt;](CalendarDaySummary.md)

## Example

```typescript
import type { CalendarSummaryResponse } from '@todorok/api-client'

// TODO: Update the object below with actual values
const example = {
  "from": null,
  "to": null,
  "days": null,
} satisfies CalendarSummaryResponse

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as CalendarSummaryResponse
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)



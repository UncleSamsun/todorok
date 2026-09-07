
# UpdateSeriesRequest


## Properties

Name | Type
------------ | -------------
`title` | string
`endDate` | string
`note` | string
`rule` | [RecurrenceRule](RecurrenceRule.md)
`version` | number

## Example

```typescript
import type { UpdateSeriesRequest } from '@todorok/api-client'

// TODO: Update the object below with actual values
const example = {
  "title": null,
  "endDate": null,
  "note": null,
  "rule": null,
  "version": null,
} satisfies UpdateSeriesRequest

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as UpdateSeriesRequest
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)



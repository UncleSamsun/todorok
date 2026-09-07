
# RecurrenceRule


## Properties

Name | Type
------------ | -------------
`frequency` | string
`interval` | number
`weekdays` | Set&lt;number&gt;
`monthDay` | number

## Example

```typescript
import type { RecurrenceRule } from '@todorok/api-client'

// TODO: Update the object below with actual values
const example = {
  "frequency": null,
  "interval": null,
  "weekdays": null,
  "monthDay": null,
} satisfies RecurrenceRule

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as RecurrenceRule
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)



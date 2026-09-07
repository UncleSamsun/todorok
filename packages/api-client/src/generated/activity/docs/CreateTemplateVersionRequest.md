
# CreateTemplateVersionRequest


## Properties

Name | Type
------------ | -------------
`commandId` | string
`expectedRevision` | number
`name` | string
`fields` | [Array&lt;FieldDefinitionInput&gt;](FieldDefinitionInput.md)

## Example

```typescript
import type { CreateTemplateVersionRequest } from '@todorok/api-client'

// TODO: Update the object below with actual values
const example = {
  "commandId": null,
  "expectedRevision": null,
  "name": null,
  "fields": null,
} satisfies CreateTemplateVersionRequest

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as CreateTemplateVersionRequest
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

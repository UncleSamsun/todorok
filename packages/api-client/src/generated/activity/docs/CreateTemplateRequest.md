
# CreateTemplateRequest


## Properties

Name | Type
------------ | -------------
`commandId` | string
`name` | string
`domain` | [TemplateDomain](TemplateDomain.md)
`kind` | [TemplateKind](TemplateKind.md)
`fields` | [Array&lt;FieldDefinitionInput&gt;](FieldDefinitionInput.md)

## Example

```typescript
import type { CreateTemplateRequest } from '@todorok/api-client'

// TODO: Update the object below with actual values
const example = {
  "commandId": null,
  "name": null,
  "domain": null,
  "kind": null,
  "fields": null,
} satisfies CreateTemplateRequest

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as CreateTemplateRequest
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


# ActivityTemplateSnapshot


## Properties

Name | Type
------------ | -------------
`schemaVersion` | number
`templateId` | string
`templateVersion` | number
`name` | string
`domain` | [TemplateDomain](TemplateDomain.md)
`kind` | [TemplateKind](TemplateKind.md)
`fields` | [Array&lt;FieldDefinition&gt;](FieldDefinition.md)

## Example

```typescript
import type { ActivityTemplateSnapshot } from '@todorok/api-client'

// TODO: Update the object below with actual values
const example = {
  "schemaVersion": null,
  "templateId": null,
  "templateVersion": null,
  "name": null,
  "domain": null,
  "kind": null,
  "fields": null,
} satisfies ActivityTemplateSnapshot

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ActivityTemplateSnapshot
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

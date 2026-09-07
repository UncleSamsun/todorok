export { validNote } from './note.js'
export interface ValidationIssue {
  field: string
  message: string
}

export type ValidationResult =
  | { valid: true }
  | { valid: false; issues: ValidationIssue[] }

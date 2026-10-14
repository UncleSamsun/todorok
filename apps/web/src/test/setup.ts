import '@testing-library/jest-dom/vitest'
import { configure } from '@testing-library/react'

// Cold route imports on Windows can exceed the default one-second DOM wait.
configure({ asyncUtilTimeout: 5000 })

package zrkn.schema.impl

import dijon.SomeJson
import zrkn.schema.impl.JsonSchemaEncoder

object JsonSchema {
  inline def apply[T: JsonSchemaEncoder]: SomeJson = JsonSchemaEncoder[T].schema

  inline def apply[T: JsonSchemaEncoder](inline dummy: T): SomeJson = apply[T]
}

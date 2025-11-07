package zrkn.schema.impl

import io.circe.Json
import zrkn.schema.impl.JsonSchemaEncoder

object JsonSchema {
  inline def apply[T: JsonSchemaEncoder]: Json = JsonSchemaEncoder[T].schema

  inline def apply[T: JsonSchemaEncoder](inline dummy: T): Json = apply[T]
}

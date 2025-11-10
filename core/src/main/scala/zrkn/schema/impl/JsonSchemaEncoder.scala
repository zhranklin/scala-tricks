package zrkn.schema.impl

import io.circe.Json

trait JsonSchemaEncoder[T] {
  def schema: Json
}

object JsonSchemaEncoder extends Auto {
  inline def ev[T](using ev: JsonSchemaEncoder[T]): JsonSchemaEncoder[T] = ev

  inline def apply[T: JsonSchemaEncoder]: JsonSchemaEncoder[T] = ev

  inline def apply[T: JsonSchemaEncoder](
    inline dummy: T
  ): JsonSchemaEncoder[T] = ev
}

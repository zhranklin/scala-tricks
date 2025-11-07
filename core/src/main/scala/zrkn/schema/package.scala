package zrkn

package object schema {
  export zrkn.schema.impl.auto.given
  export zrkn.schema.impl.{JsonSchema, JsonSchemaField, desc}
}

package schema {
  private object JsonSchemaTest {
    def main(args: Array[String]): Unit = {
      import zrkn.schema.{_, given}
      case class TestSchema(@desc("name111") name: String)
      println(JsonSchema[TestSchema])
    }
  }
}

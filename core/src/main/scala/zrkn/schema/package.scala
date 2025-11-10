package zrkn

package object schema {
  export zrkn.schema.impl.{JsonSchema, JsonSchemaField, desc}
}

package other {
  private object JsonSchemaTest {
    import zrkn.schema._
    case class TestSchema(@desc("name111") name: String)
    case class Ts(ts: List[TestSchema])

    def main(args: Array[String]): Unit = {
      println(JsonSchema[TestSchema])
      println(JsonSchema[Ts])
    }
  }
}

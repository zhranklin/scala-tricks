package zrkn.schema.impl

import zrkn.schema.impl.JsonSchemaField
import zrkn.schema.impl.JsonSchemaEncoder

import scala.annotation.nowarn
import scala.compiletime.{constValue, erasedValue, error, summonInline}
import scala.deriving.Mirror
import dijon._

trait Auto {
  import dijon.obj
  given intJsonSchemaInstance[T <: Int]: JsonSchemaEncoder[T] with {
    def schema: SomeJson = obj("type" -> "integer")
  }

  given stringJsonSchemaInstance[T <: String]: JsonSchemaEncoder[T] with {
    def schema: SomeJson = obj("type" -> "string")
  }

  given longJsonSchemaInstance[T <: Long]: JsonSchemaEncoder[T] with {
    def schema: SomeJson = obj("type" -> "integer")
  }

  given doubleJsonSchemaInstance[T <: Double]: JsonSchemaEncoder[T] with {
    def schema: SomeJson = obj("type" -> "number")
  }

  given floatJsonSchemaEncoder[T <: Float]: JsonSchemaEncoder[T] with {
    def schema: SomeJson = obj("type" -> "number")
  }

  given booleanJsonSchemaEncoder[T <: Boolean]: JsonSchemaEncoder[T] with {
    def schema: SomeJson = obj("type" -> "boolean")
  }

  given listJsonSchemaEncoder[T: JsonSchemaEncoder]: JsonSchemaEncoder[List[T]]
    with {
    def schema: SomeJson =
        obj(
          "type"  -> "array",
          "items" -> JsonSchemaEncoder[T].schema
        )
  }

  given arrayJsonSchemaEncoder[T: JsonSchemaEncoder]
  : JsonSchemaEncoder[Array[T]]
    with {
    def schema: SomeJson =
        obj(
          "type"  -> "array",
          "items" -> JsonSchemaEncoder[T].schema
        )
  }

  given nullJsonSchemaEncoder: JsonSchemaEncoder[Null] with {
    def schema: SomeJson = obj("type" -> "null")
  }

  inline private def summonLabels[Elems <: Tuple]: List[String] =
    inline erasedValue[Elems] match {
      case _: (elem *: elems) =>
        constValue[elem].toString :: summonLabels[elems]
      case _: EmptyTuple => Nil
    }

  inline private def summonInstances[T, Elems <: Tuple]
    : List[JsonSchemaEncoder[?]] =
    inline erasedValue[Elems] match {
      case _: (elem *: elems) =>
        deriveOrSummon[T, elem] :: summonInstances[T, elems]
      case _: EmptyTuple => Nil
    }

  @nowarn
  inline private def deriveOrSummon[T, Elem]: JsonSchemaEncoder[Elem] =
    inline erasedValue[Elem] match {
      case _: T => deriveRec[T, Elem]
      case _    => summonInline[JsonSchemaEncoder[Elem]]
    }

  @nowarn
  inline private def deriveRec[T, Elem]: JsonSchemaEncoder[Elem] =
    inline erasedValue[T] match {
      case _: Elem =>
        error("infinite recursive derivation")
      case _ =>
        derived[Elem](using summonInline[Mirror.Of[Elem]])
    }

  private def sumEncoder[T: Mirror.SumOf](
    elems: => List[JsonSchemaEncoder[?]],
    elemLabels: => List[String],
    childAnnotations: => Map[String, List[(String, SomeJson)]],
    typeAnnotations: => List[(String, SomeJson)]
  ): JsonSchemaEncoder[T] = new JsonSchemaEncoder[T] {
    override def schema: SomeJson =
      deepMerge(
        obj(
          "anyOf" -> arr(
            elems.zip(elemLabels).map { (elem, label) =>
              val annotations = childAnnotations.getOrElse(label, Nil)
              deepMerge(elem.schema, obj(annotations*))
            }*
          )
        ),
        obj(typeAnnotations*)
      )
  }

  def deepMerge(obj1: SomeJson, obj2: SomeJson): SomeJson = obj1.deepCopy ++ obj2.deepCopy
  private def productEncoder[T: Mirror.ProductOf](
    elems: => List[JsonSchemaEncoder[?]],
    elemLabels: => List[String],
    constructorAnnotations: => Map[String, List[(String, SomeJson)]],
    typeAnnotations: => List[(String, SomeJson)]
  ): JsonSchemaEncoder[T] =
    new JsonSchemaEncoder[T] {
      override def schema: SomeJson =
        deepMerge(

        obj(
          "type" -> "object",
          "properties" -> obj(
            elems
              .zip(elemLabels)
              .map { (elem, label) =>
                val annotations = constructorAnnotations.getOrElse(label, Nil)
                label -> deepMerge(elem.schema, obj(annotations*))
              }*
          ),
          "required" -> arr(elemLabels.map(dijon.Json(_))*),
          "additionalProperties" -> false,
        ),
        obj(typeAnnotations*)
        )
    }

  inline def derived[T](using m: Mirror.Of[T]): JsonSchemaEncoder[T] = {
    lazy val elemInstances = summonInstances[T, m.MirroredElemTypes]
    lazy val elemLabels    = summonLabels[m.MirroredElemLabels]

    inline m match {
      case s: Mirror.SumOf[T] =>
        sumEncoder(
          elemInstances,
          elemLabels,
          childAnnotations = JsonSchemaField.onChildrenOf[T],
          typeAnnotations = JsonSchemaField.onType[T]
        )(using s)

      case p: Mirror.ProductOf[T] =>
        productEncoder(
          elemInstances,
          elemLabels,
          constructorAnnotations = JsonSchemaField.onConstructorParamsOf[T],
          typeAnnotations = JsonSchemaField.onType[T]
        )(using p)
    }
  }

  inline given derivedProduct[T: Mirror.ProductOf]: JsonSchemaEncoder[T] =
    derived[T]

  inline given derivedSum[T: Mirror.SumOf]: JsonSchemaEncoder[T] =
    derived[T]
}

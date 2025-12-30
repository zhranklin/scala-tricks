package zrkn.schema.impl

import scala.annotation.{StaticAnnotation, nowarn}
import scala.quoted.*

@nowarn
class JsonSchemaField(key: String, value: dijon.SomeJson) extends StaticAnnotation

@nowarn
class desc(desc: String) extends StaticAnnotation

object JsonSchemaField {
  inline def onConstructorParamsOf[T]: Map[String, List[(String, dijon.SomeJson)]] =
    ${ onConstructorParamsOfImpl[T] }

  private def onConstructorParamsOfImpl[T: Type](using
                                                 Quotes
                                                ): Expr[Map[String, List[(String, dijon.SomeJson)]]] = {
    import quotes.reflect.*

    val tpe = TypeRepr.of[T]

    val params = tpe.typeSymbol.primaryConstructor.paramSymss

    val annotationsByParamName = params.view.flatten
      .map(param => Expr(param.name) -> extractAnnotationsForSymbol(param))
      .map((k, v) => '{ ($k, $v) })
      .toList

    '{ ${ Expr.ofList(annotationsByParamName) }.toMap }
  }

  inline def onType[T]: List[(String, dijon.SomeJson)] =
    ${ onTypeImpl[T] }

  private def onTypeImpl[T: Type](using
                                  Quotes
                                 ): Expr[List[(String, dijon.SomeJson)]] = {
    import quotes.reflect.*

    val tpe = TypeRepr.of[T]
    extractAnnotationsForSymbol(tpe.typeSymbol)
  }

  inline def onChildrenOf[T]: Map[String, List[(String, dijon.SomeJson)]] =
    ${ onChildrenOfImpl[T] }

  private def onChildrenOfImpl[T: Type](using
                                        quotes: Quotes
                                       ): Expr[Map[String, List[(String, dijon.SomeJson)]]] = {
    import quotes.reflect.*

    val tpe = TypeRepr.of[T]

    val children = tpe.typeSymbol.children

    val annotationsByChildName = children.view
      .map(child => Expr(child.name) -> extractAnnotationsForSymbol(child))
      .map((k, v) => '{ ($k, $v) })
      .toList

    '{ ${ Expr.ofList(annotationsByChildName) }.toMap }
  }

  private def extractAnnotationsForSymbol(using
                                          quotes: Quotes
                                         )(symbol: quotes.reflect.Symbol): Expr[List[(String, dijon.SomeJson)]] = {
    val annotations = symbol.annotations.view
      .map(_.asExpr)
      .collect {
        case '{ JsonSchemaField($k, $v) } => '{ ($k, $v) }
        case '{ desc($d) } => '{ ("description", dijon.Json($d)) }
      }
      .toList

    '{ ${ Expr.ofList(annotations) }.distinctBy(_._1) }
  }
}

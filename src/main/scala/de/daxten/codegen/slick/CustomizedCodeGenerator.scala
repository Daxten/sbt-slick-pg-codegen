// scalafmt: { maxColumn = 120 }
package de.daxten.codegen.slick

import slick.codegen.SourceCodeGenerator
import slick.sql.SqlProfile.ColumnOption
import slick.{model => m}

class CustomizedCodeGenerator(val model: m.Model,
                              driver: String,
                              sqlTimeType: String,
                              sqlDateType: String,
                              intervalType: String,
                              timestamptzType: String,
                              timestampType: String,
                              jsonType: String,
                              geometryType: String)
    extends SourceCodeGenerator(model) {
  override val ddlEnabled = false

  override def entityName: (String) => String =
    (dbName: String) => dbName.split("_").map(_.capitalize).mkString

  override def tableName: (String) => String = (dbName: String) => entityName(dbName) + "Table"

  override def Table = new Table(_) { table =>

    val E: String = entityName(model.name.table)
    val T: String = tableName(model.name.table)
    val Q: String = TableValue.rawName

    override def TableValue = new TableValue {
      override def rawName: String = {
        val raw = entityName(model.name.asString).uncapitalize
        if (raw.endsWith("s")) raw else raw + "s"
      }
    }

    override def autoIncLastAsOption = true

    override def EntityType = new EntityTypeDef {
      override def doc: String = ""

      override def code: String = {
        val args = columns
          .map(
            c =>
              c.default
                .map(v => s"${c.name}: ${c.exposedType} = $v")
                .getOrElse(
                  s"${c.name}: ${c.exposedType}"
              ))
          .mkString(", ")

        val prns = parents.map(" with " + _).mkString("")

        s"""
           |case class $name($args) $prns
             """.stripMargin
      }
    }

    override def Column = new Column(_) { column =>
      override def rawType: String = model.tpe match {
        case "java.sql.Date" => sqlDateType
        case "java.sql.Time" => sqlTimeType
        case "java.sql.Timestamp" =>
          model.options
            .find(_.isInstanceOf[ColumnOption.SqlType])
            .map(_.asInstanceOf[ColumnOption.SqlType].typeName)
            .map {
              case "timestamptz" => timestamptzType
              case _             => timestampType
            }
            .getOrElse(timestampType)
        case "String" =>
          model.options
            .find(_.isInstanceOf[ColumnOption.SqlType])
            .map(_.asInstanceOf[ColumnOption.SqlType].typeName)
            .map {
              case "json" | "jsonb" => jsonType
              case "hstore"         => "Map[String, String]"
              case "_text"          => "List[String]"
              case "_varchar"       => "List[String]"
              case "geometry"       => geometryType
              case "int8[]"         => "List[Long]"
              case "interval"       => intervalType
              case e                => "String"
            }
            .getOrElse("String")
        case _ => super.rawType.asInstanceOf[String]
      }

      override def code =
        s"""val $name: Rep[$actualType] = column[$actualType]("${model.name}"${options
          .filter(_ => !rawType.startsWith("List"))
          .map(", " + _)
          .mkString("")})"""

      override def rawName: String = entityName(model.name).uncapitalize
    }
  }

  override def packageCode(profile: String, pkg: String, container: String, parentType: Option[String]): String =
    s"""
       |package $pkg
       |
       |import $driver
       |import java.time._
       |import io.circe._
       |import shared.models.slick.${ExtString(container).toCamelCase}._
       |
       |object $container extends {
       |  val profile = $driver
       |} with $container
       |
       |trait $container${parentType.map(t => s" extends $t").getOrElse("")} {
       |
       |  val profile: $driver
       |  import profile.api._
       |
       |  ${indent(code.replace("$CONTAINER", container))}
       |
       |}
     """.stripMargin
}

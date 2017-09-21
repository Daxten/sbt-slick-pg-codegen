package de.daxten.codegen.slick

import sbt.Keys._
import sbt.{Def, _}

object PgCodegenPlugin extends AutoPlugin {

  object autoImport {
    val slickConfig     = settingKey[String]("")
    val migrationPrefix = settingKey[String]("")
    val schemaFolder    = settingKey[String]("")
    val schemaPackage   = settingKey[String]("")
    val modelsFolder    = settingKey[String]("")
    val modelsPackage   = settingKey[String]("")
    val customDriver    = settingKey[String]("")

    // Types
    val sqlTimeType     = settingKey[String]("")
    val sqlDateType     = settingKey[String]("")
    val intervalType    = settingKey[String]("")
    val timestamptzType = settingKey[String]("")
    val timestampType   = settingKey[String]("")
    val jsonType        = settingKey[String]("")
    val geometryType    = settingKey[String]("")

    lazy val defaultSlickCodegenSettings: Seq[Def.Setting[_]] = Seq(
      slickConfig     := "server/conf/application.conf",
      migrationPrefix := "server/conf/db/migration",
      schemaFolder    := "dbschema/src/main/scala",
      schemaPackage   := "models.slick",
      modelsFolder    := "shared/src/main/scala",
      modelsPackage   := "shared.models.slick",
      customDriver    := "driver.CustomizedPgDriver",
      sqlTimeType     := "java.time.LocalTime",
      sqlDateType     := "java.time.LocalDate",
      intervalType    := "java.time.Duration",
      timestamptzType := "java.time.OffsetDateTime",
      timestampType   := "java.time.LocalDateTime",
      jsonType        := "io.circe.Json",
      geometryType    := "com.vividsolutions.jts.geom.Geometry"
    )
  }

  import autoImport._

  override lazy val projectSettings: Seq[Def.Setting[_]] =
    inConfig(Default)(defaultSlickCodegenSettings) ++
      Seq(commands += codegenCommand.value)

  lazy val codegenCommand = Def.setting {
    Command.command("codegen") { (state: State) =>
      DbCodegen.run(
        slickConfig.value,
        migrationPrefix.value,
        schemaFolder.value,
        schemaPackage.value,
        modelsFolder.value,
        modelsPackage.value,
        customDriver.value,
        sqlTimeType.value,
        sqlDateType.value,
        intervalType.value,
        timestamptzType.value,
        timestampType.value,
        jsonType.value,
        geometryType.value,
        recreate = false
      )

      state
    }
  }
}

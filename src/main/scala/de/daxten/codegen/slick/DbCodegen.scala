// scalafmt: { maxColumn = 120 }
package de.daxten.codegen.slick

import java.io.{File => JFile}
import java.sql.DriverManager

import better.files._
import com.typesafe.config.ConfigFactory
import de.daxten.utils.{CaseClassMetaHelper, ScalaFmtHelper}
import org.flywaydb.core.Flyway

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.meta._

object DbCodegen {
  import scala.collection.JavaConversions._

  def run(configPath: String,
          migrationPrefix: String,
          schemaFolder: String,
          schemaPackage: String,
          modelsFolder: String,
          modelsPackage: String,
          driver: String,
          sqlTimeType: String,
          sqlDateType: String,
          intervalType: String,
          timestamptzType: String,
          timestampType: String,
          jsonType: String,
          geometryType: String,
          recreate: Boolean) = {

    val configFile = configPath.toFile
    println("Using Configuration File: " + configFile.pathAsString)
    val config = ConfigFactory.parseFile(configFile.toJava).resolve()

    println("# Starting databse codegeneration")
    for {
      dbConfig <- config.getObject("slick.dbs")
    } yield {
      val short = dbConfig._1
      val name = short.toString.capitalize

      val url = config.getString(s"slick.dbs.$short.db.url")
      val driver = config.getString(s"slick.dbs.$short.db.driver")
      val user = config.getString(s"slick.dbs.$short.db.user")
      val password = config.getString(s"slick.dbs.$short.db.password")

      val excluded = List("schema_version") ++ config.getStringList(s"slick.dbs.$short.db.exclude")

      val profile = slick.jdbc.PostgresProfile
      val db = profile.api.Database.forURL(url, driver = driver, user = user, password = password)

      // Load Driver for jdbc
      val driverClass = Class.forName("org.postgresql.Driver")

      if (recreate) {
        println("- Removing Database to rerun all migrations")
        val c = DriverManager.getConnection(url.reverse.dropWhile(_ != '/').reverse, user, password)
        val statement = c.createStatement()
        try {
          statement.executeUpdate(s"DROP DATABASE ${url.reverse.takeWhile(_ != '/').reverse};")
        } catch {
          case scala.util.control.NonFatal(e) => ()
        } finally {
          statement.close()
          c.close()
        }
      }

      println("- Creating Database if necessary")
      val c = DriverManager.getConnection(url.reverse.dropWhile(_ != '/').reverse, user, password)
      val statement = c.createStatement()
      try {
        statement.executeUpdate(s"CREATE DATABASE ${url.reverse.takeWhile(_ != '/').reverse};")
      } catch {
        case scala.util.control.NonFatal(e) =>
      } finally {
        statement.close()
        c.close()
      }

      println("- Migrating using flyway..")
      val flyway = new Flyway
      flyway.setClassLoader(driverClass.getClassLoader)
      flyway.setDataSource(url, user, password)
      flyway.setValidateOnMigrate(false) // Creates problems with windows machines
      flyway.setLocations(s"filesystem:$migrationPrefix/$short")
      flyway.migrate()

      println("- Starting codegeneration task..")

      def sourceGen =
        db.run(profile.createModel(Option(profile.defaultTables.map(ts =>
            ts.filterNot(t => excluded contains t.name.name)))))
          .map { model =>
            new CustomizedCodeGenerator(model,
                                        driver,
                                        sqlTimeType,
                                        sqlDateType,
                                        intervalType,
                                        timestamptzType,
                                        timestampType,
                                        jsonType,
                                        geometryType,
                                        modelsPackage)
          }

      schemaFolder.toFile.createDirectories()

      Await.ready(
        sourceGen
          .map(codegen => codegen.writeToFile(driver, schemaFolder, schemaPackage, name, s"$name.scala"))
          .recover {
            case e: Throwable => e.printStackTrace()
          },
        Duration.Inf
      )

      val schemaPackageAsPath = schemaPackage.replace('.', '/')

      println("- Parsing generated slick-model")
      val createdFile = file"$schemaFolder/$schemaPackageAsPath/$name.scala"
      val modelSource = createdFile.contentAsString
      val sharedCaseClasses =
        modelSource.split("\n").map(_.trim).filter(_.startsWith("case class"))
      val filteredSource = modelSource
        .split("\n")
        .filterNot(_.trim.startsWith("case class"))
        .mkString("\n")
      println("Saving filtered slick-model")
      createdFile.overwrite(filteredSource)

      val modelsPackageAsPath = modelsPackage.replace('.', '/')
      // Creating Shared Models
      val path =
        file"$modelsFolder/$modelsPackageAsPath/${name.toCamelCase}"
      path.createDirectories()
      sharedCaseClasses.foreach { caseClass =>
        val caseClassStat = caseClass.parse[Stat].get
        val modelName = caseClassStat.collect {
          case q"case class $tname (...$paramss)" =>
            tname.value
        }.head

        val targetFile = path./(s"$modelName.scala")

        if (targetFile.notExists) {
          println(s"-- Creating ${targetFile.path.toString}")

          val template =
            s"""
               |package $schemaPackage.${name.toCamelCase}
               |
               |$caseClass
          """.trim.stripMargin

          targetFile.createIfNotExists(createParents = true).overwrite(template)
        } else {
          println(s"-- Loading ${targetFile.path.toString}")

          val source = targetFile.contentAsString.parse[Source].get
          val tree = CaseClassMetaHelper.updateOrInsert(source, caseClassStat)
          targetFile.write(ScalaFmtHelper.formatCode(tree.syntax))
        }
      }
    }
  }
}

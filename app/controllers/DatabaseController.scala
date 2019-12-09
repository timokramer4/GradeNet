package controllers

import anorm.{RowParser, SQL, SqlParser}
import com.typesafe.config.ConfigFactory
import javax.inject.Inject
import play.api.db.DBApi
import play.api.mvc.ControllerComponents

class DatabaseController @Inject()(dbapi: DBApi, cc: ControllerComponents) {

  // Get database connection
  private val db = dbapi.database("default")

  // Check database integrity
  def checkDBIntegrity(): Unit = {
    db.withConnection { implicit c =>
      ConfigFactory.load().getString("db.default.driver") match {
        case "com.mysql.jdbc.Driver" => // MySQL
          var success: Boolean =
            SQL("CREATE TABLE IF NOT EXISTS appreciation (" +
              "id INT(11) PRIMARY KEY AUTO_INCREMENT," +
              "firstName VARCHAR(255) NOT NULL," +
              "lastName VARCHAR(255) NOT NULL," +
              "email VARCHAR(255) NOT NULL," +
              "matrNr INT(11) NOT NULL," +
              "university INT(11) NOT NULL," +
              "state INT(11) NOT NULL" +
              ");").execute()
        case "org.postgresql.Driver" => // PostgreSQL
          var success: Boolean =
            SQL("CREATE TABLE IF NOT EXISTS appreciation (" +
              "id SERIAL PRIMARY KEY," +
              "firstName CHAR(255) NOT NULL," +
              "lastName CHAR(255) NOT NULL," +
              "email CHAR(255) NOT NULL," +
              "matrNr INT NOT NULL," +
              "university INT NOT NULL," +
              "state INT NOT NULL" +
              ");").execute()
        case _ => println("No database driver selected!")
      }
    }
  }

  // Database entity definition
  case class Appreciation(id: Option[Long] = None,
                          firstName: String,
                          lastName: String,
                          email: String,
                          matrNr: Int,
                          university: Int,
                          state: Int
                         )

  // Create new appreciation in database
  def createAppreciation(firstName: String, lastName: String, email: String, matrNr: Int, university: Int): Long = {
    checkDBIntegrity()
    db.withConnection { implicit c =>
      val id: Option[Long] =
        SQL("INSERT INTO Appreciation (firstName, lastName, email, matrNr, university, state) values ({firstName}, {lastName}, {email}, {matrNr}, {university}, 0)")
          .on("firstName" -> firstName, "lastName" -> lastName, "email" -> email, "matrNr" -> matrNr, "university" -> university)
          .executeInsert()
      return id.getOrElse(0)
    }
  }

  // Remove specific appreciation
  def removeAppreciation(id: Long, decrement: Boolean): Unit = {
    checkDBIntegrity()
    db.withConnection { implicit c =>
      val amountDelete: Int =
        SQL(s"DELETE FROM appreciation WHERE id = ${id}")
          .executeUpdate()
      if (decrement) {
        println("CurrentID" + id)
        ConfigFactory.load().getString("db.default.driver") match {
          case "com.mysql.jdbc.Driver" => // MySQL
            val decrementSuccess: Boolean =
              SQL(s"ALTER TABLE appreciation AUTO_INCREMENT=${
                if (id > 1) {
                  id - 1
                } else {
                  id
                }
              };").execute()
          case "org.postgresql.Driver" => // PostgreSQL
            val decrementSuccess: Boolean =
              SQL(s"ALTER SEQUENCE appreciation_id_seq RESTART WITH ${
                if (id > 1) {
                  id - 1
                } else {
                  id
                }
              };").execute()
          case _ => println("No database driver selected!")
        }
      }
    }
  }

  // Get all appreciations
  def getAllAppreciations(): List[Map[String, Any]] = {
    checkDBIntegrity()
    db.withConnection { implicit c =>
      val parser: RowParser[Map[String, Any]] =
        SqlParser.folder(Map.empty[String, Any]) { (map, value, meta) =>
          Right(map + (meta.column.qualified -> value))
        }

      val result: List[Map[String, Any]] = {
        SQL("SELECT * from appreciation").as(parser.*)
      }

      return result
    }
  }

  // Get single appreciation
  def getSingleAppreciation(id: Int): Map[String, Any] = {
    checkDBIntegrity()
    db.withConnection { implicit c =>
      val parser: RowParser[Map[String, Any]] =
        SqlParser.folder(Map.empty[String, Any]) { (map, value, meta) =>
          Right(map + (meta.column.qualified -> value))
        }

      val result: List[Map[String, Any]] = {
        SQL(s"SELECT * from appreciation WHERE id = ${id}").as(parser.*)
      }

      return result(0)
    }
  }
}

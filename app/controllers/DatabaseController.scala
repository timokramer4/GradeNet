package controllers

import anorm.SQL
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
              "university INT(11) NOT NULL" +
              ");").execute()
        case "org.postgresql.Driver" => // PostgreSQL
          var success: Boolean =
            SQL("CREATE TABLE IF NOT EXISTS appreciation (" +
              "id SERIAL PRIMARY KEY," +
              "firstName CHAR(255) NOT NULL," +
              "lastName CHAR(255) NOT NULL," +
              "email CHAR(255) NOT NULL," +
              "matrNr INT NOT NULL," +
              "university INT NOT NULL" +
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
                         )

  // Create new appreciation in database
  def createAppreciation(firstName: String, lastName: String, email: String, matrNr: Int, university: Int): Long = {
    checkDBIntegrity()
    db.withConnection { implicit c =>
      val id: Option[Long] =
        SQL("INSERT INTO Appreciation (firstName, lastName, email, matrNr, university) values ({firstName}, {lastName}, {email}, {matrNr}, {university})")
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
      if(decrement){
        println("CurrentID" + id)
        ConfigFactory.load().getString("db.default.driver") match {
          case "com.mysql.jdbc.Driver" => // MySQL
            val decrementSuccess: Boolean =
              SQL(s"ALTER TABLE appreciation AUTO_INCREMENT=${if(id>1){id-1}else{id}};").execute()
          case "org.postgresql.Driver" => // PostgreSQL
            val decrementSuccess: Boolean =
              SQL(s"ALTER SEQUENCE appreciation_id_seq RESTART WITH ${if(id>1){id-1}else{id}};").execute()
          case _ => println("No database driver selected!")
        }
      }
    }
  }
}

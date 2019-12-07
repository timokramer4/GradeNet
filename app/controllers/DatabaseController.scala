package controllers

import anorm.SQL
import javax.inject.Inject
import play.api.db.DBApi
import play.api.mvc.ControllerComponents

class DatabaseController @Inject()(dbapi: DBApi, cc: ControllerComponents) {

  // Get database connection
  private val db = dbapi.database("default")

  // Check database integrity
  def checkDBIntegrity(): Unit = {
    db.withConnection { implicit c =>
      var success: Boolean =
        SQL("CREATE TABLE IF NOT EXISTS appreciation (" +
          "id INT(11) PRIMARY KEY AUTO_INCREMENT," +
          "firstName VARCHAR(255) NOT NULL," +
          "lastName VARCHAR(255) NOT NULL," +
          "email VARCHAR(255) NOT NULL," +
          "matrNr INT(11) NOT NULL," +
          "university INT(11) NOT NULL" +
          ");").execute()
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
        SQL(s"insert into Appreciation (firstName, lastName, email, matrNr, university) values ({firstName}, {lastName}, {email}, {matrNr}, {university})")
          .on("firstName" -> firstName, "lastName" -> lastName, "email" -> email, "matrNr" -> matrNr, "university" -> university)
          .executeInsert()
      return id.getOrElse(0)
    }
  }

  // Remove specific appreciation
  def removeAppreciation(id: Long): Unit ={
    checkDBIntegrity()
    db.withConnection { implicit c =>
      val amountDelete: Int =
        SQL(s"DELETE FROM appreciation WHERE id = ${id}")
          .executeUpdate()
    }
  }
}

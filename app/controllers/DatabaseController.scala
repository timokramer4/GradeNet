package controllers

import anorm.SQL
import javax.inject.Inject
import play.api.db.DBApi
import play.api.mvc.ControllerComponents

class DatabaseController @Inject()(dbapi: DBApi, cc: ControllerComponents){

  // Get database connection
  private val db = dbapi.database("default")

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
    db.withConnection { implicit c =>
      val id: Option[Long] =
        SQL("insert into Appreciation (firstName, lastName, email, matrNr, university) values ({firstName}, {lastName}, {email}, {matrNr}, {university})")
          .on("firstName" -> firstName, "lastName" -> lastName, "email" -> email, "matrNr" -> matrNr, "university" -> university)
          .executeInsert()
      return id.getOrElse(0)
    }
  }
}

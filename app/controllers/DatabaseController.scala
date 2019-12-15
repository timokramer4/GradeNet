package controllers

import anorm.SqlParser._
import anorm.{RowParser, SQL, _}
import com.typesafe.config.ConfigFactory
import javax.inject.Inject
import models.State._
import models.{State, Student, User}
import play.api.db.DBApi
import play.api.mvc.ControllerComponents

class DatabaseController @Inject()(dbapi: DBApi, cc: ControllerComponents) {

  // Get database connection
  private val db = dbapi.database("default")

  private val appreciationEntity = "appreciation"
  private val userEntity = "user"

  // Check database integrity
  def checkDBIntegrity(): Unit = {
    db.withConnection { implicit c =>
      ConfigFactory.load().getString("db.default.driver") match {
        case "com.mysql.jdbc.Driver" => // MySQL
          var success: Boolean =
            SQL(
              s"""CREATE TABLE IF NOT EXISTS ${appreciationEntity} (
              id INT(11) PRIMARY KEY AUTO_INCREMENT,
              firstName VARCHAR(255) NOT NULL,
              lastName VARCHAR(255) NOT NULL,
              email VARCHAR(255) NOT NULL,
              matrNr INT(11) NOT NULL,
              university INT(11) NOT NULL,
              state INT(11) NOT NULL
              );""").execute()
          success =
            SQL(
              s"""CREATE TABLE IF NOT EXISTS ${userEntity} (
              username VARCHAR(255) PRIMARY KEY,
              password VARCHAR(255) NOT NULL,
              admin BOOLEAN NOT NULL
              );""").execute()
          success = SQL(
            s"""INSERT INTO ${userEntity} (username, password, admin) VALUES ("admin", "52edc26fb9842cb6a77de4c2f27709d8a3d812545a5852ba9dbef35d10a7a5c5", 1) ON DUPLICATE KEY UPDATE username=username;""").execute()
        case "org.postgresql.Driver" => // PostgreSQL
          var success: Boolean =
            SQL(
              s"""CREATE TABLE IF NOT EXISTS "${appreciationEntity}" (
              id SERIAL PRIMARY KEY,
              firstName VARCHAR NOT NULL,
              lastName VARCHAR NOT NULL,
              email VARCHAR NOT NULL,
              matrNr INT NOT NULL,
              university INT NOT NULL,
              state INT NOT NULL
              );""").execute()
          success =
            SQL(
              s"""CREATE TABLE IF NOT EXISTS "${userEntity}" (
              username VARCHAR PRIMARY KEY,
              password VARCHAR NOT NULL,
              admin BOOLEAN NOT NULL
              )""").execute()
          success = SQL(
            s"""INSERT INTO "${userEntity}" (username, password, admin) VALUES ('admin', '52edc26fb9842cb6a77de4c2f27709d8a3d812545a5852ba9dbef35d10a7a5c5', TRUE) ON CONFLICT (username) DO NOTHING;""").execute()
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
      var id: Option[Long] = Some(0)
      ConfigFactory.load().getString("db.default.driver") match {
        case "com.mysql.jdbc.Driver" => // MySQL
          id = SQL(s"""INSERT INTO ${appreciationEntity} (firstName, lastName, email, matrNr, university, state) values ({firstName}, {lastName}, {email}, {matrNr}, {university}, 0)""")
            .on("firstName" -> firstName, "lastName" -> lastName, "email" -> email, "matrNr" -> matrNr, "university" -> university)
            .executeInsert()
        case "org.postgresql.Driver" => // PostgreSQL
          id = SQL(s"""INSERT INTO "${appreciationEntity}" (firstName, lastName, email, matrNr, university, state) values ({firstName}, {lastName}, {email}, {matrNr}, {university}, 0)""")
            .on("firstName" -> firstName, "lastName" -> lastName, "email" -> email, "matrNr" -> matrNr, "university" -> university)
            .executeInsert()
      }
      return id.getOrElse(0)
    }
  }

  // Change state of existing appreciation
  def changeAppreciationState(id: Int, state: Int): Long = {
    checkDBIntegrity()
    db.withConnection { implicit c =>
      var amountUpdated: Int = 0
      ConfigFactory.load().getString("db.default.driver") match {
        case "com.mysql.jdbc.Driver" => // MySQL
          amountUpdated = SQL(s"""UPDATE ${appreciationEntity} SET state = ${state} WHERE id = ${id}""")
            .executeUpdate()
        case "org.postgresql.Driver" => // PostgreSQL
          amountUpdated = SQL(s"""UPDATE "${appreciationEntity}" SET state = ${state} WHERE id = ${id}""")
            .executeUpdate()
      }
      return amountUpdated
    }
  }

  // Remove specific appreciation
  def removeAppreciation(id: Long, decrement: Boolean): Unit = {
    checkDBIntegrity()
    db.withConnection { implicit c =>
      val amountDelete: Int =
        SQL(s"""DELETE FROM "${appreciationEntity}" WHERE id = ${id}""")
          .executeUpdate()
      if (decrement) {
        println("CurrentID" + id)
        ConfigFactory.load().getString("db.default.driver") match {
          case "com.mysql.jdbc.Driver" => // MySQL
            val decrementSuccess: Boolean =
              SQL(
                s"""ALTER TABLE ${appreciationEntity} AUTO_INCREMENT=${
                  if (id > 1) {
                    id - 1
                  } else {
                    id
                  }
                };""").execute()
          case "org.postgresql.Driver" => // PostgreSQL
            val decrementSuccess: Boolean =
              SQL(
                s"""ALTER SEQUENCE "${appreciationEntity}_id_seq" RESTART WITH ${
                  if (id > 1) {
                    id - 1
                  } else {
                    id
                  }
                };""").execute()
        }
      }
    }
  }

  // Get all appreciations
  def getAllAppreciations(): List[Student] = {
    checkDBIntegrity()

    db.withConnection { implicit c =>
      val parser: RowParser[Student] =
        int("id") ~ str("firstname") ~ str("lastname") ~ int("matrnr") ~ str("email") ~ int("university") ~ int("state") map {
          case id ~ fn ~ ln ~ mnr ~ email ~ uni ~ state => Student(id, fn, ln, mnr, email, uni, switchStateInt(state).asInstanceOf[State])
        }

      val result: List[Student] = {
        ConfigFactory.load().getString("db.default.driver") match {
          case "com.mysql.jdbc.Driver" => // MySQL
            SQL(s"""SELECT * FROM ${appreciationEntity}""").as(parser.*)
          case "org.postgresql.Driver" => // PostgreSQL
            SQL(s"""SELECT * FROM "${appreciationEntity}"""").as(parser.*)
        }

      }

      return result
    }
  }

  // Get single appreciation
  def getSingleAppreciation(id: Int): Student = {
    checkDBIntegrity()
    db.withConnection { implicit c =>
      val parser: RowParser[Student] = {
        int("id") ~ str("firstname") ~ str("lastname") ~ int("matrnr") ~ str("email") ~ int("university") ~ int("state") map {
          case id ~ fn ~ ln ~ mnr ~ email ~ uni ~ state => Student(id, fn, ln, mnr, email, uni, switchStateInt(state).asInstanceOf[State])
        }
      }

      val result: Student = {
        ConfigFactory.load().getString("db.default.driver") match {
          case "com.mysql.jdbc.Driver" => // MySQL
            SQL(s"""SELECT * from ${appreciationEntity} WHERE id = ${id}""").as(parser.single)
          case "org.postgresql.Driver" => // PostgreSQL
            SQL(s"""SELECT * from "${appreciationEntity}" WHERE id = ${id}""").as(parser.single)
        }
      }

      return result
    }
  }

  def getUser(username: String): User = {
    checkDBIntegrity()
    db.withConnection { implicit c =>
      val parser: RowParser[User] = {
        str("username") ~ str("password") ~ bool("admin") map {
          case username ~ password ~ admin => User(username, password, admin)
        }
      }

      val result: User = {
        try {
          ConfigFactory.load().getString("db.default.driver") match {
            case "com.mysql.jdbc.Driver" => // MySQL
              SQL(s"""SELECT * FROM ${userEntity} WHERE username = '${username}' LIMIT 1""").as(parser.single)
            case "org.postgresql.Driver" => // PostgreSQL
              SQL(s"""SELECT * FROM "${userEntity}" WHERE username = '${username}' LIMIT 1""").as(parser.single)
          }
        }
        catch {
          case e: Exception => {
            println(s"""Benutzer "${username}" nicht gefunden!""")
            User("", "", false)
          }
        }
      }

      return result
    }
  }
}

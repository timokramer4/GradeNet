package controllers

import anorm.SqlParser._
import anorm.{RowParser, SQL, _}
import com.typesafe.config.ConfigFactory
import javax.inject.Inject
import models.State._
import models.{Module, State, Appreciation, User}
import play.api.db.DBApi
import play.api.mvc.ControllerComponents

class DatabaseController @Inject()(dbapi: DBApi, cc: ControllerComponents) {

  // Get database connection
  private val db = dbapi.database("default")

  private val appreciationEntity = "appreciation"
  private val modulesEntity = "modules"
  private val appreciationModulesEntity = s"${appreciationEntity}_${modulesEntity}"
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
              university VARCHAR(255) NOT NULL,
              state INT(11) NOT NULL
              );""").execute()
          success = SQL(
            s"""CREATE TABLE IF NOT EXISTS ${modulesEntity} (
               id INT(11) PRIMARY KEY AUTO_INCREMENT,
               name VARCHAR NOT NULL
               );""").execute()
          success = SQL(
            s"""CREATE TABLE IF NOT EXISTS "${appreciationModulesEntity}" (
               appreciation_id SERIAL REFERENCES ${appreciationEntity}(id),
               module_id SERIAL REFERENCES ${modulesEntity}(id)
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
              university VARCHAR NOT NULL,
              state INT NOT NULL
              );""").execute()
          success = SQL(
            s"""CREATE TABLE IF NOT EXISTS "${modulesEntity}" (
               id SERIAL PRIMARY KEY,
               name VARCHAR NOT NULL
               );""").execute()
          success = SQL(
            s"""CREATE TABLE IF NOT EXISTS "${appreciationEntity}_${modulesEntity}" (
               appreciation_id SERIAL REFERENCES ${appreciationEntity}(id),
               module_id SERIAL REFERENCES ${modulesEntity}(id)
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

  // Create new appreciation in database
  def createAppreciation(firstName: String, lastName: String, email: String, matrNr: Int, university: String): Long = {
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

  // Append module to created appreciation
  def appendModuleToAppreciation(aId: Long, mId: Int): Long = {
    checkDBIntegrity()
    db.withConnection { implicit c =>
      var id: Option[Long] = Some(0)
      ConfigFactory.load().getString("db.default.driver") match {
        case "com.mysql.jdbc.Driver" => // MySQL
          id = SQL(s"""INSERT INTO ${appreciationModulesEntity} (appreciation_id, module_id) values (${aId}, ${mId})""")
            .executeInsert()
        case "org.postgresql.Driver" => // PostgreSQL
          id = SQL(s"""INSERT INTO "${appreciationModulesEntity}" (appreciation_id, module_id) values (${aId}, ${mId})""")
            .executeInsert()
      }
      return id.getOrElse(0)
    }
  }

  // Remove specific appreciation
  def removeAppreciation(id: Long, decrement: Boolean): Unit = {
    checkDBIntegrity()
    db.withConnection { implicit c =>
      val amountDelete: Int =
        SQL(
          s"""DELETE FROM "${appreciationEntity}" WHERE id = ${id};
          DELETE FROM ${appreciationModulesEntity} WHERE appreciation_id = ${id}""")
          .executeUpdate()
      if (decrement) {
        println("CurrentID: " + id)
        ConfigFactory.load().getString("db.default.driver") match {
          case "com.mysql.jdbc.Driver" => // MySQL
            val decrementSuccess: Boolean =
              SQL(
                s"""ALTER TABLE ${appreciationEntity} AUTO_INCREMENT=${
                  if (id > 1) {
                    id - 1
                  } else {
                    1
                  }
                };""").execute()
          case "org.postgresql.Driver" => // PostgreSQL
            val decrementSuccess: Boolean =
              SQL(
                s"""ALTER SEQUENCE "${appreciationEntity}_id_seq" RESTART WITH ${
                  if (id > 1) {
                    id - 1
                  } else {
                    1
                  }
                };""").execute()
        }
      }
    }
  }

  // Get all appreciations
  def getAllAppreciations(): List[Appreciation] = {
    checkDBIntegrity()

    db.withConnection { implicit c =>
      val parser: RowParser[Appreciation] =
        int("id") ~ str("firstname") ~ str("lastname") ~ int("matrnr") ~ str("email") ~ str("university") ~ int("state") map {
          case id ~ fn ~ ln ~ mnr ~ email ~ uni ~ state => Appreciation(id, fn, ln, mnr, email, uni, switchStateInt(state).asInstanceOf[State])
        }

      val result: List[Appreciation] = {
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
  def getSingleAppreciation(id: Int): Appreciation = {
    checkDBIntegrity()
    db.withConnection { implicit c =>
      val parser: RowParser[Appreciation] = {
        int("id") ~ str("firstname") ~ str("lastname") ~ int("matrnr") ~ str("email") ~ str("university") ~ int("state") map {
          case id ~ fn ~ ln ~ mnr ~ email ~ uni ~ state => Appreciation(id, fn, ln, mnr, email, uni, switchStateInt(state).asInstanceOf[State])
        }
      }

      val result: Appreciation = {
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

  def getModulesFromAppreciation(id: Int): List[Module] = {
    checkDBIntegrity()
    db.withConnection {
      implicit c =>
        val parser: RowParser[Module] = {
          int("id") ~ str("name") map {
            case id ~ name => Module(id, name)
          }
        }

        val result: List[Module] = {
          ConfigFactory.load().getString("db.default.driver") match {
            case "com.mysql.jdbc.Driver" => // MySQL
              SQL(
                s"""SELECT * from ${
                  appreciationEntity
                } WHERE id = ${
                  id
                }""").as(parser.*)
            case "org.postgresql.Driver" => // PostgreSQL
              SQL(
                s"""SELECT ${
                  modulesEntity
                }.id, ${
                  modulesEntity
                }.name from "${
                  appreciationEntity
                }" JOIN "${
                  appreciationModulesEntity
                }" ON ("${
                  appreciationEntity
                }".id = "${
                  appreciationModulesEntity
                }".appreciation_id) JOIN "${
                  modulesEntity
                }" ON ("${
                  appreciationModulesEntity
                }".module_id = "${
                  modulesEntity
                }".id) WHERE "${
                  appreciationEntity
                }".id = ${
                  id
                }""").as(parser.*)
          }
        }
        return result
    }
  }

  def getModules(): List[Module] = {
    checkDBIntegrity()
    db.withConnection {
      implicit c =>
        val parser: RowParser[Module] = {
          int("id") ~ str("name") map {
            case id ~ name => Module(id, name)
          }
        }

        val result: List[Module] = {
          try {
            ConfigFactory.load().getString("db.default.driver") match {
              case "com.mysql.jdbc.Driver" => // MySQL
                SQL(
                  s"""SELECT * FROM ${
                    modulesEntity
                  }""").as(parser.*)
              case "org.postgresql.Driver" => // PostgreSQL
                SQL(
                  s"""SELECT * FROM "${
                    modulesEntity
                  }"""").as(parser.*)
            }
          }
          catch {
            case e: Exception => {
              println(s"""Es konnten keine Module gefunden werden!""")
              List(Module(0, ""))
            }
          }
        }

        return result
    }
  }

  def getModuleFromIntList(intList: List[Int]): List[Module] = {
    checkDBIntegrity()
    db.withConnection {
      implicit c =>
        val parser: RowParser[Module] = {
          int("id") ~ str("name") map {
            case id ~ name => Module(id, name)
          }
        }

        var moduleList: List[Module] = List[Module]()
        try {
          ConfigFactory.load().getString("db.default.driver") match {
            case "com.mysql.jdbc.Driver" => { // MySQL
              intList.foreach(moduleId =>
                moduleList = SQL(
                  s"""SELECT * FROM ${
                    modulesEntity
                  } WHERE id = ${
                    moduleId
                  }""").as(parser.single) :: moduleList
              )
            }
            case "org.postgresql.Driver" => { // PostgreSQL
              intList.foreach {
                moduleId =>
                  moduleList = SQL(
                    s"""SELECT * FROM "${
                      modulesEntity
                    }" WHERE id = ${
                      moduleId
                    }""").as(parser.single) :: moduleList
              }
            }
          }
        }
        catch {
          case e: Exception => {
            println(s"""Es konnten keine Module gefunden werden!""")
            List(Module(0, ""))
          }
        }
        return moduleList
    }
  }

  def getUser(username: String): User = {
    checkDBIntegrity()
    db.withConnection {
      implicit c =>
        val parser: RowParser[User] = {
          str("username") ~ str("password") ~ bool("admin") map {
            case username ~ password ~ admin => User(username, password, admin)
          }
        }

        val result: User = {
          try {
            ConfigFactory.load().getString("db.default.driver") match {
              case "com.mysql.jdbc.Driver" => // MySQL
                SQL(
                  s"""SELECT * FROM ${
                    userEntity
                  } WHERE username = '${
                    username
                  }' LIMIT 1""").as(parser.single)
              case "org.postgresql.Driver" => // PostgreSQL
                SQL(
                  s"""SELECT * FROM "${
                    userEntity
                  }" WHERE username = '${
                    username
                  }' LIMIT 1""").as(parser.single)
            }
          }
          catch {
            case e: Exception => {
              println(
                s"""Benutzer "${
                  username
                }" nicht gefunden!""")
              User("", "", false)
            }
          }
        }

        return result
    }
  }
}

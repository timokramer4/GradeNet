package controllers

import anorm.SqlParser._
import anorm.{RowParser, SQL, _}
import com.typesafe.config.ConfigFactory
import javax.inject.Inject
import models.State._
import models.{Appreciation, Course, Module, State, User}
import play.api.db.DBApi
import play.api.mvc.ControllerComponents

class DatabaseController @Inject()(dbapi: DBApi, cc: ControllerComponents) {

  // Get database connection
  private val db = dbapi.database("default")

  private val appreciationEntity = "appreciation"
  private val modulesEntity = "modules"
  private val appreciationModulesEntity = s"${appreciationEntity}_${modulesEntity}"
  private val coursesEntity = "course"
  private val userEntity = "user"

  /**
   * Check database integrity
   */
  def checkDBIntegrity(): Unit = {
    db.withConnection { implicit c =>
      ConfigFactory.load().getString("db.default.driver") match {
        case "com.mysql.jdbc.Driver" => // MySQL
          var success: Boolean = SQL(
            s"""CREATE TABLE IF NOT EXISTS "${coursesEntity}" (
               id SERIAL PRIMARY KEY,
               name VARCHAR(255) NOT NULL,
               po INT(11) NOT NULL,
               gradiation INT NOT NULL,
               semester INT NOT NULL
               );""").execute()
          success = SQL(
            s"""CREATE TABLE IF NOT EXISTS ${appreciationEntity} (
              id SERIAL PRIMARY KEY,
              firstName VARCHAR(255) NOT NULL,
              lastName VARCHAR(255) NOT NULL,
              email VARCHAR(255) NOT NULL,
              matrNr INT(11) NOT NULL,
              university VARCHAR(255) NOT NULL,
              state INT(11) NOT NULL,
              currentpo SERIAL REFERENCES ${coursesEntity}(id),
              newpo SERIAL REFERENCES ${coursesEntity}(id),
              password VARCHAR(255) NOT NULL
              );""").execute()
          success = SQL(
            s"""CREATE TABLE IF NOT EXISTS ${modulesEntity} (
               id SERIAL PRIMARY KEY,
               name VARCHAR NOT NULL,
               semester INT(11) NOT NULL,
               course_id SERIAL REFERENCES ${coursesEntity}(id)
               );""").execute()
          success = SQL(
            s"""CREATE TABLE IF NOT EXISTS "${appreciationModulesEntity}" (
               appreciation_id SERIAL REFERENCES ${appreciationEntity}(id),
               module_id SERIAL REFERENCES ${modulesEntity}(id),
               appreciationname VARCHAR(255) NOT NULL
               );""").execute()
          success =
            SQL(
              s"""CREATE TABLE IF NOT EXISTS ${userEntity} (
              id SERIAL PRIMARY KEY,
              username VARCHAR(255) UNIQUE NOT NULL,
              password VARCHAR(255) NOT NULL,
              admin BOOLEAN NOT NULL
              );""").execute()
          success = SQL(
            s"""INSERT INTO ${userEntity} (username, password, admin) VALUES ("admin", "52edc26fb9842cb6a77de4c2f27709d8a3d812545a5852ba9dbef35d10a7a5c5", 1) ON DUPLICATE KEY UPDATE username=username;""").execute()
        case "org.postgresql.Driver" => // PostgreSQL
          var success: Boolean = SQL(
            s"""CREATE TABLE IF NOT EXISTS "${coursesEntity}" (
               id SERIAL PRIMARY KEY,
               name VARCHAR NOT NULL,
               po INT NOT NULL,
               gradiation INT NOT NULL,
               semester INT NOT NULL
               );""").execute()
          success = SQL(
            s"""CREATE TABLE IF NOT EXISTS "${appreciationEntity}" (
              id SERIAL PRIMARY KEY,
              firstName VARCHAR NOT NULL,
              lastName VARCHAR NOT NULL,
              email VARCHAR NOT NULL,
              matrNr INT NOT NULL,
              university VARCHAR NOT NULL,
              state INT NOT NULL,
              currentpo SERIAL REFERENCES ${coursesEntity}(id),
              newpo SERIAL REFERENCES ${coursesEntity}(id),
              password VARCHAR NOT NULL
              );""").execute()
          success = SQL(
            s"""CREATE TABLE IF NOT EXISTS "${modulesEntity}" (
               id SERIAL PRIMARY KEY,
               name VARCHAR NOT NULL,
               semester INT NOT NULL,
               course_id SERIAL REFERENCES ${coursesEntity}(id)
               );""").execute()
          success = SQL(
            s"""CREATE TABLE IF NOT EXISTS "${appreciationEntity}_${modulesEntity}" (
               appreciation_id SERIAL REFERENCES ${appreciationEntity}(id),
               module_id SERIAL REFERENCES ${modulesEntity}(id),
               appreciationname VARCHAR NOT NULL
               );""").execute()
          success =
            SQL(
              s"""CREATE TABLE IF NOT EXISTS "${userEntity}" (
              id SERIAL PRIMARY KEY,
              username VARCHAR UNIQUE NOT NULL,
              password VARCHAR NOT NULL,
              admin BOOLEAN NOT NULL
              );""").execute()
          success = SQL(
            s"""INSERT INTO "${userEntity}" (username, password, admin) VALUES ('admin', '52edc26fb9842cb6a77de4c2f27709d8a3d812545a5852ba9dbef35d10a7a5c5', TRUE) ON CONFLICT (username) DO NOTHING;""").execute()
      }
    }
  }

  /** *************************
   * * APPRECIATIONS
   * * *************************/

  /**
   * Create new appreciation in database
   *
   * @param firstName
   * @param lastName
   * @param email
   * @param matrNr
   * @param university
   * @return
   */
  def createAppreciation(firstName: String, lastName: String, email: String, matrNr: Int, university: String, currentPO: Int, newPO: Option[Int], passwordHash: String): Long = {
    checkDBIntegrity()
    db.withConnection { implicit c =>
      var id: Option[Long] = Some(0)
      ConfigFactory.load().getString("db.default.driver") match {
        case "com.mysql.jdbc.Driver" => // MySQL
          if (newPO == None) {
            id = SQL(s"""INSERT INTO ${appreciationEntity} (firstName, lastName, email, matrNr, university, currentpo, newpo, password, state) VALUES ({firstName}, {lastName}, {email}, {matrNr}, {university}, {currentPO}, {newPO}, {password}, 0)""")
              .on("firstName" -> firstName, "lastName" -> lastName, "email" -> email, "matrNr" -> matrNr, "university" -> university, "currentPO" -> currentPO, "newPO" -> currentPO, "password" -> passwordHash)
              .executeInsert()
          } else {
            id = SQL(s"""INSERT INTO ${appreciationEntity} (firstName, lastName, email, matrNr, university, currentpo, newpo, password, state) VALUES ({firstName}, {lastName}, {email}, {matrNr}, {university}, {currentPO}, {newPO}, {password}, 0)""")
              .on("firstName" -> firstName, "lastName" -> lastName, "email" -> email, "matrNr" -> matrNr, "university" -> university, "currentPO" -> currentPO, "newPO" -> newPO.get, "password" -> passwordHash)
              .executeInsert()
          }
        case "org.postgresql.Driver" => // PostgreSQL
          if (newPO == None) {
            id = SQL(s"""INSERT INTO "${appreciationEntity}" (firstName, lastName, email, matrNr, university, currentpo, newpo, password, state) VALUES ({firstName}, {lastName}, {email}, {matrNr}, {university}, {currentPO}, {newPO}, {password}, 0)""")
              .on("firstName" -> firstName, "lastName" -> lastName, "email" -> email, "matrNr" -> matrNr, "university" -> university, "currentPO" -> currentPO, "newPO" -> currentPO, "password" -> passwordHash)
              .executeInsert()
          } else {
            id = SQL(s"""INSERT INTO "${appreciationEntity}" (firstName, lastName, email, matrNr, university, currentpo, newpo, password, state) VALUES ({firstName}, {lastName}, {email}, {matrNr}, {university}, {currentPO}, {newPO}, {password}, 0)""")
              .on("firstName" -> firstName, "lastName" -> lastName, "email" -> email, "matrNr" -> matrNr, "university" -> university, "currentPO" -> currentPO, "newPO" -> newPO.get, "password" -> passwordHash)
              .executeInsert()
          }
      }
      return id.getOrElse(0)
    }
  }

  /**
   * Change state of existing appreciation
   *
   * @param id
   * @param state
   * @return
   */
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

  /**
   * Remove specific appreciation
   *
   * @param id
   * @param decrement
   */
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

  /**
   * Get all appreciations
   *
   * @return
   */
  def getAllAppreciations(): List[Appreciation] = {
    checkDBIntegrity()
    db.withConnection { implicit c =>
      val parser: RowParser[Appreciation] =
        int("id") ~ str("firstname") ~ str("lastname") ~ int("matrnr") ~ str("email") ~ str("university") ~ int("currentpo") ~ int("newpo") ~ str("password")  ~ int("state") map {
          case id ~ fn ~ ln ~ mnr ~ email ~ uni ~ currentPO ~ newPO ~ password ~ state => Appreciation(id, fn, ln, mnr, email, uni, currentPO, newPO, password, switchStateInt(state).asInstanceOf[State])
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

  /**
   * Get single appreciation
   *
   * @param id
   * @return
   */
  def getSingleAppreciation(id: Int): Appreciation = {
    checkDBIntegrity()
    db.withConnection { implicit c =>
      val parser: RowParser[Appreciation] = {
        int("id") ~ str("firstname") ~ str("lastname") ~ int("matrnr") ~ str("email") ~ str("university") ~ int("currentpo") ~ int("newpo") ~ str("password") ~ int("state") map {
          case id ~ fn ~ ln ~ mnr ~ email ~ uni ~ currentPO ~ newPO ~ password ~ state => Appreciation(id, fn, ln, mnr, email, uni, currentPO, newPO, password, switchStateInt(state).asInstanceOf[State])
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

  /**
   * Append module to created appreciation
   *
   * @param aId
   * @param mId
   * @return
   */
  def appendModuleToAppreciation(aId: Long, mId: Int, appreciationName: String): Long = {
    checkDBIntegrity()
    db.withConnection { implicit c =>
      var id: Option[Long] = Some(0)
      ConfigFactory.load().getString("db.default.driver") match {
        case "com.mysql.jdbc.Driver" => // MySQL
          id = SQL(s"""INSERT INTO ${appreciationModulesEntity} (appreciation_id, module_id, appreciationname) values (${aId}, ${mId}, '${appreciationName}')""")
            .executeInsert()
        case "org.postgresql.Driver" => // PostgreSQL
          id = SQL(s"""INSERT INTO "${appreciationModulesEntity}" (appreciation_id, module_id, appreciationname) values (${aId}, ${mId}, '${appreciationName}')""")
            .executeInsert()
      }
      return id.getOrElse(0)
    }
  }

  /**
   * Get selected modules in appreciation
   *
   * @param id
   * @return
   */
  def getModulesFromAppreciation(id: Int): List[Module] = {
    checkDBIntegrity()
    db.withConnection {
      implicit c =>
        val parser: RowParser[Module] = {
          int("id") ~ str("name") ~ get[Option[String]]("appreciationName") ~ int("semester") ~ int("course_id") map {
            case id ~ name ~ appreciationName ~ semester ~ course => Module(id, name, appreciationName, semester, course)
          }
        }

        val result: List[Module] = {
          ConfigFactory.load().getString("db.default.driver") match {
            case "com.mysql.jdbc.Driver" => // MySQL
              SQL(
                s"""SELECT * from ${
                  appreciationEntity
                } JOIN ${
                  appreciationModulesEntity
                } ON (${
                  appreciationEntity
                }.id = ${
                  appreciationModulesEntity
                }.appreciation_id) JOIN ${
                  modulesEntity
                } ON (${
                  appreciationModulesEntity
                }.module_id = ${
                  modulesEntity
                }.id) WHERE ${
                  appreciationEntity
                }.id = ${
                  id
                }""").as(parser.*)
            case "org.postgresql.Driver" => // PostgreSQL
              SQL(
                s"""SELECT * from "${
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

  /** *************************
   * * COURSES
   * * *************************/

  /**
   * Create new course entry
   *
   * @param name
   * @param gradiation
   * @param semester
   * @return
   */
  def createCourse(name: String, po: Int, gradiation: Int, semester: Int): Long = {
    checkDBIntegrity()
    db.withConnection {
      implicit c =>
        var id: Option[Long] = Some(0)
        ConfigFactory.load().getString("db.default.driver") match {
          case "com.mysql.jdbc.Driver" => // MySQL
            id = SQL(
              s"""INSERT INTO ${
                coursesEntity
              } (name, po, gradiation, semester) values ({name}, {po}, {gradiation}, {semester})""")
              .on("name" -> name, "po" -> po, "gradiation" -> gradiation, "semester" -> semester)
              .executeInsert()
          case "org.postgresql.Driver" => // PostgreSQL
            id = SQL(
              s"""INSERT INTO "${
                coursesEntity
              }" (name, po, gradiation, semester) values ({name}, {po}, {gradiation}, {semester})""")
              .on("name" -> name, "po" -> po, "gradiation" -> gradiation, "semester" -> semester)
              .executeInsert()
        }
        return id.getOrElse(0)
    }
  }

  /**
   * Edit course
   *
   * @param course
   * @return
   */
  def editCourse(course: Course): Int = {
    checkDBIntegrity()
    db.withConnection {
      implicit c =>
        var amountEdited: Int = 0
        ConfigFactory.load().getString("db.default.driver") match {
          case "com.mysql.jdbc.Driver" => // MySQL
            amountEdited = SQL(
              s"""UPDATE ${
                coursesEntity
              } SET name = '${
                course.name
              }', gradiation = ${
                course.graduation
              }, semester = ${
                course.semester
              } WHERE id = ${
                course.id
              };""").executeUpdate()
          case "org.postgresql.Driver" => // PostgreSQL
            amountEdited = SQL(
              s"""UPDATE "${
                coursesEntity
              }" SET name = '${
                course.name
              }', gradiation = ${
                course.graduation
              }, semester = ${
                course.semester
              } WHERE id = ${
                course.id
              };""").executeUpdate()
        }
        return amountEdited
    }
  }

  /**
   * Remove course
   *
   * @param id
   * @return
   */
  def removeCourse(id: Int): Int = {
    checkDBIntegrity()
    db.withConnection {
      implicit c =>
        var amountDelete: Int = 0
        ConfigFactory.load().getString("db.default.driver") match {
          case "com.mysql.jdbc.Driver" => // MySQL
            SQL(
              s"""DELETE FROM ${appreciationEntity} WHERE course_id = ${id};
                 DELETE FROM ${modulesEntity} WHERE course_id = ${id};
                 """).executeUpdate()
            amountDelete = SQL(s"""DELETE FROM ${coursesEntity} WHERE id = ${id};""").executeUpdate()
          case "org.postgresql.Driver" => // PostgreSQL
            SQL(
              s"""DELETE FROM "${appreciationEntity}" WHERE course_id = ${id};
                 DELETE FROM "${modulesEntity}" WHERE course_id = ${id};
                 """).executeUpdate()
            amountDelete = SQL(s"""DELETE FROM "${coursesEntity}" WHERE id = ${id};""").executeUpdate()
        }
        return amountDelete
    }
  }

  /**
   * Get single course from database
   *
   * @param id
   * @return
   */
  def getCourse(id: Int): Course = {
    checkDBIntegrity()
    db.withConnection {
      implicit c =>
        val parser: RowParser[Course] = {
          int("id") ~ str("name") ~ int("po") ~ int("gradiation") ~ int("semester") map {
            case id ~ name ~ po ~ gradiation ~ semester => Course(id, name, po, gradiation, semester)
          }
        }

        val result: Course = {
          ConfigFactory.load().getString("db.default.driver") match {
            case "com.mysql.jdbc.Driver" => // MySQL
              SQL(
                s"""SELECT * FROM ${
                  coursesEntity
                } WHERE id = ${
                  id
                };""").as(parser.single)
            case "org.postgresql.Driver" => // PostgreSQL
              SQL(
                s"""SELECT * FROM "${
                  coursesEntity
                }" WHERE id = ${
                  id
                };""").as(parser.single)
          }
        }
        return result
    }
  }

  /**
   * Get all courses from database
   *
   * @return
   */
  def getAllCourses(): List[Course] = {
    checkDBIntegrity()
    db.withConnection {
      implicit c =>
        val parser: RowParser[Course] = {
          int("id") ~ str("name") ~ int("po") ~ int("gradiation") ~ int("semester") map {
            case id ~ name ~ po ~ gradiation ~ semester => Course(id, name, po, gradiation, semester)
          }
        }

        val result: List[Course] = {
          try {
            ConfigFactory.load().getString("db.default.driver") match {
              case "com.mysql.jdbc.Driver" => // MySQL
                SQL(
                  s"""SELECT * FROM ${
                    coursesEntity
                  } ORDER BY name ASC;""").as(parser.*)
              case "org.postgresql.Driver" => // PostgreSQL
                SQL(
                  s"""SELECT * FROM "${
                    coursesEntity
                  }" ORDER BY name ASC;""").as(parser.*)
            }
          }
          catch {
            case e: Exception => {
              println(s"""Es konnten keine Studiengänge gefunden werden!""")
              List()
            }
          }
        }
        return result
    }
  }

  /** *************************
   * * MODULES
   * * *************************/

  /**
   * Create new module
   *
   * @param name
   * @param semester
   * @param course
   * @return
   */
  def createModule(name: String, semester: Int, course: Int): Long = {
    checkDBIntegrity()
    db.withConnection {
      implicit c =>
        var id: Option[Long] = Some(0)
        ConfigFactory.load().getString("db.default.driver") match {
          case "com.mysql.jdbc.Driver" => // MySQL
            id = SQL(
              s"""INSERT INTO ${
                modulesEntity
              } (name, semester, course_id) values ({name}, {semester}, {course})""")
              .on("name" -> name, "semester" -> semester, "course" -> course)
              .executeInsert()
          case "org.postgresql.Driver" => // PostgreSQL
            id = SQL(
              s"""INSERT INTO "${
                modulesEntity
              }" (name, semester, course_id) values ({name}, {semester}, {course})""")
              .on("name" -> name, "semester" -> semester, "course" -> course)
              .executeInsert()
        }
        return id.getOrElse(0)
    }
  }

  /**
   * Edit existing module
   *
   * @param module
   * @return
   */
  def editModule(module: Module): Int = {
    checkDBIntegrity()
    db.withConnection {
      implicit c =>
        var amountEdited: Int = 0
        ConfigFactory.load().getString("db.default.driver") match {
          case "com.mysql.jdbc.Driver" => // MySQL
            amountEdited = SQL(
              s"""UPDATE ${modulesEntity} SET name = '${module.name}', semester = ${module.semester}, course_id = ${module.course} WHERE id = ${module.id};""").executeUpdate()
          case "org.postgresql.Driver" => // PostgreSQL
            amountEdited = SQL(
              s"""UPDATE "${modulesEntity}" SET name = '${module.name}', semester = ${module.semester}, course_id = ${module.course} WHERE id = ${module.id};""").executeUpdate()
        }
        return amountEdited
    }
  }

  /**
   * Remove specific module
   *
   * @param id
   * @return
   */
  def removeModule(id: Int): Long = {
    checkDBIntegrity()
    db.withConnection {
      implicit c =>
        var amountDelete: Int = 0
        ConfigFactory.load().getString("db.default.driver") match {
          case "com.mysql.jdbc.Driver" => // MySQL
            SQL(
              s"""DELETE FROM ${
                appreciationEntity
              } WHERE module_id = ${
                id
              };""").executeUpdate()
            amountDelete = SQL(
              s"""DELETE FROM ${
                modulesEntity
              } WHERE id = ${
                id
              };""").executeUpdate()
          case "org.postgresql.Driver" => // PostgreSQL
            SQL(
              s"""DELETE FROM "${
                appreciationModulesEntity
              }" WHERE module_id = ${
                id
              };""").executeUpdate()
            amountDelete = SQL(
              s"""DELETE FROM "${
                modulesEntity
              }" WHERE id = ${
                id
              };""").executeUpdate()
            removeAppreciation(id, false)
        }
        return amountDelete
    }
  }

  /**
   * Get single module
   *
   * @param id
   * @return
   */
  def getModule(id: Int): Module = {
    checkDBIntegrity()
    db.withConnection {
      implicit c =>
        val parser: RowParser[Module] = {
          int("id") ~ str("name") ~ int("semester") ~ int("course_id") map {
            case id ~ name ~ semester ~ course => Module(id, name, None, semester, course)
          }
        }

        val result: Module = {
          ConfigFactory.load().getString("db.default.driver") match {
            case "com.mysql.jdbc.Driver" => // MySQL
              SQL(
                s"""SELECT * FROM ${
                  modulesEntity
                } WHERE id = ${id}""").as(parser.single)
            case "org.postgresql.Driver" => // PostgreSQL
              SQL(
                s"""SELECT * FROM "${
                  modulesEntity
                }" WHERE id = ${id}""").as(parser.single)
          }
        }

        return result
    }
  }

  /**
   * Get all modules from database
   *
   * @param courseId
   * @return
   */
  def getAllModules(courseId: Int): List[Module] = {
    checkDBIntegrity()
    db.withConnection {
      implicit c =>
        val parser: RowParser[Module] = {
          int("id") ~ str("name") ~ int("semester") ~ int("course_id") map {
            case id ~ name ~ semester ~ course => Module(id, name, None, semester, course)
          }
        }

        val result: List[Module] = {
          try {
            ConfigFactory.load().getString("db.default.driver") match {
              case "com.mysql.jdbc.Driver" => // MySQL
                SQL(
                  s"""SELECT * FROM ${
                    modulesEntity
                  } WHERE course_id = ${courseId}""").as(parser.*)
              case "org.postgresql.Driver" => // PostgreSQL
                SQL(
                  s"""SELECT * FROM "${
                    modulesEntity
                  }" WHERE course_id = ${courseId}""").as(parser.*)
            }
          }
          catch {
            case e: Exception => {
              println(s"""Es konnten keine Module gefunden werden!""")
              List()
            }
          }
        }
        return result
    }
  }

  /**
   * Convert module list from int to module list
   *
   * @param intList
   * @return
   */
  def getModuleFromIntList(intList: List[Int]): List[Module] = {
    checkDBIntegrity()
    db.withConnection {
      implicit c =>
        val parser: RowParser[Module] = {
          int("id") ~ str("name") ~ int("semester") ~ int("course_id") map {
            case id ~ name ~ semester ~ course => Module(id, name, None, semester, course)
          }
        }

        var moduleList: List[Module] = List[Module]()
        try {
          ConfigFactory.load().getString("db.default.driver") match {
            case "com.mysql.jdbc.Driver" => { // MySQL
              intList.foreach(moduleId =>
                moduleList = moduleList :+ SQL(
                  s"""SELECT * FROM ${
                    modulesEntity
                  } WHERE id = ${
                    moduleId
                  }""").as(parser.single)
              )
            }
            case "org.postgresql.Driver" => { // PostgreSQL
              intList.foreach {
                moduleId =>
                  moduleList = moduleList :+ SQL(
                    s"""SELECT * FROM "${
                      modulesEntity
                    }" WHERE id = ${
                      moduleId
                    }""").as(parser.single)
              }
            }
          }
        }
        catch {
          case e: Exception => {
            println(s"""Es konnten keine Module gefunden werden!""")
            List()
          }
        }
        return moduleList
    }
  }

  /** *************************
   * * USERS
   * * *************************/

  /**
   * Create new user
   *
   * @param username
   * @param passwordHash
   * @param admin
   * @return
   */
  def createUser(username: String, passwordHash: String, admin: Boolean): Long = {
    checkDBIntegrity()
    db.withConnection {
      implicit c =>
        var id: Option[Long] = Some(0)
        ConfigFactory.load().getString("db.default.driver") match {
          case "com.mysql.jdbc.Driver" => // MySQL
            id = SQL(s"""INSERT INTO ${userEntity} (username, password, admin) values ({username}, {password}, {admin})""")
              .on("username" -> username, "password" -> passwordHash, "admin" -> admin)
              .executeInsert()
          case "org.postgresql.Driver" => // PostgreSQL
            id = SQL(s"""INSERT INTO "${userEntity}" (username, password, admin) values ({username}, {password}, {admin})""")
              .on("username" -> username, "password" -> passwordHash, "admin" -> admin)
              .executeInsert()
        }
        return id.getOrElse(0)
    }
  }

  /**
   * Get specific user from database
   *
   * @param username
   * @return
   */
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

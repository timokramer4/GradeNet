package controllers

import java.io.File
import java.nio.file.{Files, Path, Paths}

import controllers.Forms.AppreciationForm._
import controllers.Forms.StateForm._
import controllers.Forms.CourseForm._
import controllers.Forms.CourseForm
import controllers.Forms.LoginForm._
import javax.inject._
import models.State._
import models.{Appreciation, Course, Module, User}
import play.api.libs.json.{JsArray, JsObject, JsString, JsValue, Json}
import play.api.mvc.{Action, AnyContent, _}
import controllers.Hasher.generateHash
import play.twirl.api.Html

import scala.concurrent.ExecutionContext
import scala.reflect.io.Directory

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */

@Singleton
class HomeController @Inject()(dbController: DatabaseController, cc: ControllerComponents) extends AbstractController(cc) with play.api.i18n.I18nSupport {

  // Define default upload parameters
  var tmpUploadDir: Path = _
  val uploadDir: String = "uploads"

  // GET: Landing page
  def home(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.main("Startseite", views.html.home()))
  }

  // GET: Appreciation single grades
  def appreciationSingle(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    val r = requests.get("http://universities.hipolabs.com/search?country=germany")
    val uniList: List[(String, String)] = jsonConverter(Json.parse(r.text)) // [{}, {}]
    val moduleList: List[(String, String)] = dbController.getModules().map(module => (module.id.toString, module.name))
    val courseList: List[(String, String)] = dbController.getAllCourses().map(course => (course.id.toString, s"${course.name} - ${Course.getGraduation(course)}"))
    Ok(views.html.main("Antrag", views.html.appreciationSingle(aFormSingle, uniList, moduleList, courseList)))
  }

  // POST: Form appreciation single grades
  def appreciationSinglePost = Action(parse.multipartFormData) { implicit request =>
    aFormSingle.bindFromRequest.fold(
      errorForm => {
        Redirect(routes.HomeController.appreciationSingle).flashing("error" -> "Fehlende Angaben! Bitte füllen Sie alle notwendigen Felder aus.")
      },
      successForm => {
        var i: Int = 0
        var firstFile: Boolean = true
        var petitionId: Long = 0
        object State {
          val SUCCESS = 0
          val MISSING_FILES = 1
          val WRONG_FILE_TYPE = 2
        }
        var formError: Int = State.SUCCESS

        val moduleList: List[Module] = dbController.getModuleFromIntList(successForm.modules)

        // Check amount of files
        if (moduleList.size == request.body.files.size - 1) {
          request.body.files.foreach { file =>
            // Iterate if there are no errors in form data
            if (formError == State.SUCCESS) {
              // Only get the last part of the filename without path
              val fileArray: Array[String] = Paths.get(file.filename).getFileName.toString().split('.')
              val fileType: String = fileArray(fileArray.length - 1)

              // First File: Grading file
              if (firstFile) {
                if (fileType == "pdf") {
                  // Create upload dir, if not exists
                  if (!Files.exists(Paths.get(uploadDir))) {
                    Files.createDirectory(Paths.get(uploadDir))
                  }

                  // Create new appreciation in database
                  petitionId = dbController.createAppreciation(successForm.firstName, successForm.lastName, successForm.email, successForm.matrNr, successForm.university, successForm.course)

                  // Remove existing directory recursive
                  if (Files.exists(Paths.get(s"$uploadDir/$petitionId"))) {
                    val dir = new Directory(new File(s"$uploadDir/$petitionId"))
                    dir.deleteRecursively()
                  }

                  // Create new directory
                  tmpUploadDir = Files.createDirectory(Paths.get(s"$uploadDir/$petitionId"))

                  // Upload file in new directory
                  file.ref.moveFileTo(Paths.get(s"$tmpUploadDir/Notenkonto.$fileType"), replace = false)
                  firstFile = false
                } else {
                  formError = State.WRONG_FILE_TYPE
                }
              }
              // Other files: Module descriptions
              else {
                if (fileType == "pdf") {
                  // Create module instance
                  dbController.appendModuleToAppreciation(petitionId, moduleList(i - 1).id)

                  // Upload file in new directory
                  file.ref.moveFileTo(Paths.get(s"$tmpUploadDir/${moduleList(i - 1).name}.$fileType"), replace = false)
                } else {
                  formError = State.WRONG_FILE_TYPE
                }
              }
            }
            i += 1
          }
        }
        // Too few files uploaded
        else {
          formError = State.MISSING_FILES
        }

        // Determine the specific error
        if (formError != State.SUCCESS) {
          // Remove all appreciation data in database
          dbController.removeAppreciation(petitionId, true)

          // Remove existing directory recursive
          if (Files.exists(Paths.get(s"$uploadDir/$petitionId"))) {
            val dir = new Directory(new File(s"$uploadDir/$petitionId"))
            dir.deleteRecursively()
          }

          // Error: No file uploaded
          if (formError == State.MISSING_FILES) {
            Redirect(routes.HomeController.appreciationSingle).flashing("error" -> "Einer oder mehrere fehlende Anhänge. Jeder Antrag muss min. ein Notenkonto und für jedes Modul eine Beschreibung im PDF-Format enthalten!")
          }
          // Error: No valid file format
          else if (formError == State.WRONG_FILE_TYPE) {
            Redirect(routes.HomeController.appreciationSingle).flashing("error" -> "Es sind nur PDF-Dateien als Anhang erlaubt!")
          }
          // Unknown error
          else {
            Redirect(routes.HomeController.appreciationSingle).flashing("error" -> "Ein unbekannter Fehler ist aufgetreten!")
          }
        }
        // Success: If all data correct
        else {
          Redirect(routes.HomeController.appreciationSingle).flashing("success" -> "Der Antrag wurde erfolgreich eingereicht!")
        }
      }
    )
  }

  // GET: Appreciation all grades
  def appreciationAll(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    val r = requests.get("http://universities.hipolabs.com/search?country=germany")
    val uniList: List[(String, String)] = jsonConverter(Json.parse(r.text))
    val courseList: List[(String, String)] = dbController.getAllCourses().map(course => (course.id.toString, s"${course.name} - ${Course.getGraduation(course)}"))
    Ok(views.html.main("Antrag", views.html.appreciationAll(aFormAll, uniList, courseList)))
  }

  // POST: Form appreciation all grades
  def appreciationAllPost = Action(parse.multipartFormData) { implicit request =>
    aFormAll.bindFromRequest.fold(
      errorForm => {
        Redirect(routes.HomeController.appreciationAll).flashing("error" -> "Fehlende Angaben! Bitte füllen Sie alle notwendigen Felder aus.")
        //        BadRequest(views.html.appreciationAll(errorForm))
      },
      successForm => {
        request.body
          .file("gradeFile")
          .map { file =>
            // Only get the last part of the filename without path
            val fileArray: Array[String] = Paths.get(file.filename).getFileName().toString().split('.')
            val fileType: String = fileArray(fileArray.length - 1)
            var petitionId: Long = 0

            if (fileType == "pdf") {
              // Create upload dir, if not exists
              if (!Files.exists(Paths.get(uploadDir))) {
                Files.createDirectory(Paths.get(uploadDir))
              }

              // Create new appreciation in database
              petitionId = dbController.createAppreciation(successForm.firstName, successForm.lastName, successForm.email, successForm.matrNr, successForm.university, successForm.course)

              // Remove existing directory recursive
              if (Files.exists(Paths.get(s"$uploadDir/$petitionId"))) {
                val dir = new Directory(new File(s"$uploadDir/$petitionId"))
                dir.deleteRecursively()
              }

              // Create new directory
              tmpUploadDir = Files.createDirectory(Paths.get(s"$uploadDir/$petitionId"))

              // Upload file in new directory
              file.ref.moveFileTo(Paths.get(s"$tmpUploadDir/Notenkonto.$fileType"), replace = false)

              // Redirect and show success alert after successfully transfer all form data
              Redirect(routes.HomeController.appreciationAll).flashing("success" -> "Der Antrag wurde erfolgreich eingereicht!")
            } else {
              Redirect(routes.HomeController.appreciationAll).flashing("error" -> "Es sind nur PDF-Dateien als Anhang erlaubt! Bitte laden Sie Ihr Notenkonto als PDF Datei hoch!")
            }
          }
          .getOrElse {
            // Redirect and show error
            Redirect(routes.HomeController.appreciationAll).flashing("error" -> "Ein Antrag ohne Notenkonto ist nicht möglich! Bitte laden Sie Ihr Notenkonto hoch.")
          }
      }
    )
  }

  def showCurrentState(id: Int): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    val appreciation: Appreciation = dbController.getSingleAppreciation(id)
    Ok(views.html.main("Status", views.html.state(appreciation)))
  }

  // GET: Login page
  def loginPage: Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.main("Anmeldung", views.html.login()))
  }

  // POST: Login user
  def login: Action[AnyContent] = Action(parse.anyContent) { implicit request =>
    loginForm.bindFromRequest.fold(
      errorForm => {
        Redirect(routes.HomeController.loginPage()).flashing("error" -> "Fehlende Angaben!")
      },
      successForm => {
        val user: User = dbController.getUser(successForm.username)
        val passInput: String = generateHash(successForm.password)
        println("Input Hash: " + passInput)
        println("DB Hash: " + user.password)
        println("Admin: " + user.admin)
        if (user.password == passInput) {
          if (user.admin) {
            println("Logged in successfully!")
            Redirect(routes.HomeController.adminPanel()).withSession("connected" -> user.username)
          } else {
            println("No permissions!")
            Redirect(routes.HomeController.loginPage()).flashing("error" -> "Fehlende Berechtigungen!")
          }
        } else {
          println("Access denied!")
          Redirect(routes.HomeController.loginPage()).flashing("error" -> "Benutzername oder Passwort falsch!")
        }
      }
    )
  }

  // GET: Logout current logged user
  def logout: Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Redirect(routes.HomeController.home()).withNewSession.flashing("success" -> "Sie wurden erfolgreich abgemeldet!")
  }

  // GET: Admin panel
  def adminPanel: Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    if (checkLogin(request)) {
      val data: List[Appreciation] = dbController.getAllAppreciations()
      Ok(views.html.main("Admin Panel", views.html.adminPanel(data)))
    } else {
      Redirect(routes.HomeController.loginPage())
    }
  }


  // GET: Admin panel details
  def adminPanelDetails(id: Int): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    if (checkLogin(request)) {
      val appreciationData: Appreciation = dbController.getSingleAppreciation(id)
      val uploadedFiles: List[File] = getListOfFiles(id)
      val stateList: List[Int] = getStateList()
      val moduleList: List[Module] = dbController.getModulesFromAppreciation(id)
      Ok(views.html.main("Admin Panel", views.html.adminPanelDetails(appreciationData, uploadedFiles, stateList, moduleList)))
    } else {
      Redirect(routes.HomeController.loginPage())
    }
  }

  // POST: Change appreciation state
  def adminPanelDetailsChangeState(id: Int): Action[AnyContent] = Action(parse.anyContent) { implicit request =>
    if (checkLogin(request)) {
      stateForm.bindFromRequest.fold(
        errorForm => {
          Redirect(routes.HomeController.adminPanelDetails(id)).flashing("error" -> "Fehler beim Ändern des Status!")
        },
        successForm => {
          // Change state in database
          dbController.changeAppreciationState(id, successForm.state)

          // Redirect after change and show changes
          Redirect(routes.HomeController.adminPanelDetails(id)).flashing("success" -> s"""Status von Antrag #${id} wurde erfolgreich auf "${stateToString(successForm.state)}" geändert!""")
        }
      )
    } else {
      Redirect(routes.HomeController.loginPage())
    }
  }

  // GET: AdminPanel courses
  def adminPanelCourses: Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    if (checkLogin(request)) {
      val coursesList: List[Course] = dbController.getAllCourses()
      Ok(views.html.main("Admin Panel", views.html.adminPanelCourses(coursesList)))
    } else {
      Redirect(routes.HomeController.loginPage())
    }
  }

  // POST: Create new posts
  def adminPanelCoursesPost = Action(parse.multipartFormData) { implicit request =>
    courseForm.bindFromRequest.fold(
      errorForm => {
        Redirect(routes.HomeController.appreciationAll).flashing("error" -> "Fehlende Angaben! Bitte füllen Sie alle notwendigen Felder aus.")
        //        BadRequest(views.html.appreciationAll(errorForm))
      },
      successForm => {
        // Create new appreciation in database
        dbController.createCourse(successForm.name, successForm.gradiation, successForm.semester)

        // Redirect and show success alert after successfully transfer all form data
        Redirect(routes.HomeController.adminPanelCourses()).flashing("success" -> s"""Der Studiengang "${successForm.name}" wurde erfolgreich angelegt!""")
      }
    )
  }

  def adminPanelSingleCourseRemove(id: Int): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    // Create new appreciation in database
    if (dbController.removeCourse(id) > 0) {
      // Redirect and show success alert after successfully transfer all form data
      Redirect(routes.HomeController.adminPanelCourses()).flashing("success" -> s"""Der Studiengang mit der ID "${id}" wurde erfolgreich entfernt!""")
    } else {
      // Redirect and show success alert after successfully transfer all form data
      Redirect(routes.HomeController.adminPanelCourses()).flashing("success" -> s"""Der Studiengang mit der ID "${id}" wurde erfolgreich entfernt!""")
    }
  }

  def adminPanelSingleCourse(id: Int): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    if (checkLogin(request)) {
      val course: Course = dbController.getSingleCourse(id)
      val filledForm = courseForm.fill(CourseForm.Data(course.name, course.graduation, course.semester))
      Ok(views.html.main("Admin Panel", views.html.adminPanelSingleCourse(id, filledForm)))
    } else {
      Redirect(routes.HomeController.loginPage())
    }
  }

  def adminPanelSingleCoursePost(id: Int): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    if (checkLogin(request)) {
      courseForm.bindFromRequest.fold(
        errorForm => {
          println(errorForm.errors)
          Redirect(routes.HomeController.adminPanelSingleCourse(id)).flashing("error" -> "Fehlende Angaben! Bitte füllen Sie alle notwendigen Felder aus.")
        },
        successForm => {
          // Create new appreciation in database
          dbController.editCourse(Course(id, successForm.name, successForm.gradiation, successForm.semester))

          // Redirect and show success alert after successfully transfer all form data
          Redirect(routes.HomeController.adminPanelSingleCourse(id)).flashing("success" -> s"""Der Studiengang "${successForm.name}" wurde erfolgreich aktualisiert!""")
        }
      )
    } else {
      Redirect(routes.HomeController.loginPage())
    }
  }

  // GET: Download a specific file
  def downloadFile(id: Int, fileName: String): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    if (checkLogin(request)) {
      implicit val ec = ExecutionContext.global
      val appreciationData: Appreciation = dbController.getSingleAppreciation(id)
      Ok.sendFile(
        content = new java.io.File(s"${uploadDir}/${id}/${fileName}"),
        fileName = _ => s"Antrag#${id}_${appreciationData.lastName}_${appreciationData.firstName}_${fileName.replace(' ', '_')}",
        inline = false
      )
    } else {
      Redirect(routes.HomeController.loginPage())
    }
  }

  // ======================
  // Helper Functions
  // ======================

  // Convert JSON Array in (value, content) pair for select field
  def jsonConverter(jsValue: JsValue): List[(String, String)] = {
    jsValue match {
      case JsArray(jsArray) => jsArray.map(v => (extract(v, "name"), extract(v, "name"))).toList
      case _ => throw new IllegalArgumentException("JSON hat unerwartete Struktur!")
    }
  }

  // Extract JSON property
  def extract(v: JsValue, property: String): String = {
    v match {
      case JsObject(map) => map.get(property) match {
        case Some(JsString(str)) => str
        case _ => throw new IllegalArgumentException("JSON hat unerwartete Struktur!")
      }
      case _ => throw new IllegalArgumentException("JSON hat unerwartete Struktur!")
    }
  }

  // Return list of all files
  def getListOfFiles(id: Int): List[File] = {
    val dir = new File(s"${uploadDir}/${id}")
    if (dir.exists && dir.isDirectory) {
      dir.listFiles.filter(_.isFile).toList
    } else {
      List[File]()
    }
  }

  // Check login session
  def checkLogin(request: Request[AnyContent]): Boolean = {
    request.session
      .get("connected")
      .map { loggedUser =>
        return true
      }.getOrElse {
      return false
    }
  }
}
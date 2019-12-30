package controllers

import java.io.File
import java.nio.file.{Files, Path, Paths}

import controllers.Forms.AppreciationForm._
import controllers.Forms.StateForm._
import controllers.Forms.CourseForm._
import controllers.Forms.ModuleForm._
import controllers.Forms.LoginForm._
import javax.inject._
import models.State._
import models.{Appreciation, Course, Module, User}
import play.api.libs.json.{JsArray, JsObject, JsString, JsValue, Json, Writes}
import play.api.mvc.{Action, AnyContent, _}
import controllers.Hasher._
import play.api.libs.mailer.{Email, MailerClient}

import scala.concurrent.ExecutionContext
import scala.reflect.io.Directory

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */

@Singleton
class HomeController @Inject()(dbController: DatabaseController, cc: ControllerComponents, mailerClient: MailerClient) extends AbstractController(cc) with play.api.i18n.I18nSupport {

  // Define default upload parameters
  var tmpUploadDir: Path = _
  val uploadDir: String = "uploads"
  val rootAddress: String = "http://localhost:9000"

  /**
   * Send mail
   *
   * @param subject
   * @param receiverMail
   * @param receiverFirstName
   * @param receiverLastName
   * @param txtMsg
   * @param htmlMsg
   * @return
   */
  def sendEmail(subject: String, receiverMail: String, receiverFirstName: String, receiverLastName: String, txtMsg: String, htmlMsg: String) = {
    val email = Email(
      subject,
      "GradeNet <gradenet@timoskramkiste.de>",
      Seq(s"${receiverFirstName} ${receiverLastName} <${receiverMail}>"),

      bodyText = Some(txtMsg),
      bodyHtml = Some(htmlMsg)
    )
    mailerClient.send(email)
  }

  /**
   * GET: Landing page
   *
   * @return
   */
  def home(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.main("Startseite", views.html.home()))
  }

  /**
   * GET: Appreciation single grades
   *
   * @return
   */
  def appreciationSingle(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    val r = requests.get("http://universities.hipolabs.com/search?country=germany")
    val uniList: List[(String, String)] = jsonConverter(Json.parse(r.text)) // [{}, {}]
    val moduleList: List[(String, String)] = dbController.getAllModules(0).map(module => (module.id.toString, module.name))
    val courseList: List[(String, String)] = dbController.getAllCourses().map(course => (course.id.toString, s"${course.name} - ${Course.getGraduation(course)}"))
    Ok(views.html.main("Antrag", views.html.appreciationSingle(aFormSingle, uniList, moduleList, courseList)))
  }

  /**
   * POST: Form appreciation single grades
   *
   * @return
   */
  def appreciationSinglePost = Action(parse.multipartFormData) { implicit request =>
    aFormSingle.bindFromRequest.fold(
      errorForm => {
        println(errorForm.errors.toList)
        Redirect(routes.HomeController.appreciationSingle).flashing("error" -> "Fehlende Angaben! Bitte füllen Sie alle notwendigen Felder aus.")
      },
      successForm => {
        var i: Int = 0
        var firstFile: Boolean = true
        var petitionId: Long = 0
        var randomPassword: String = ""
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
                  randomPassword = generateRandomPassword(None)
                  petitionId = dbController.createAppreciation(successForm.firstName, successForm.lastName, successForm.email, successForm.matrNr, successForm.university, None, None, generateHash(randomPassword, false), successForm.course)

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
                  dbController.appendModuleToAppreciation(petitionId, moduleList(i - 1).id, successForm.appreciationModules(i - 1))

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
          sendEmail(
            s"Antrag #${petitionId} eigegangen",
            successForm.email,
            successForm.firstName,
            successForm.lastName,
            s"",
            s"<html><body><p><b>Hallo ${successForm.firstName} ${successForm.lastName},</b><br>Ihr Antrag zur Notenanerkennung ist bei uns eingenagngen " +
              s"und wird so schnell wie möglich bearbeitet. Den Status Ihres Antrags können Sie mit folgenden Zugangsdaten einsehen:</p>" +
              s"<p><b>Link:</b> <a href='${rootAddress}${routes.HomeController.showCurrentState(petitionId.toInt)}'>${rootAddress}${routes.HomeController.showCurrentState(petitionId.toInt)}</a><br>" +
              s"<b>Passwort:</b> <code>${randomPassword}</code></p>" +
              s"<p><b>Mit freundlichen Grüßen<br>GradeNet</b></p></html><body>")
          Redirect(routes.HomeController.showCurrentState(petitionId.toInt)).withSession((s"appreciation${petitionId}", petitionId.toString)).flashing("success" -> "Der Antrag wurde erfolgreich eingereicht!")
        }
      }
    )
  }

  /**
   * GET: Appreciation all grades
   *
   * @return
   */
  def appreciationAll(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    val r = requests.get("http://universities.hipolabs.com/search?country=germany")
    val uniList: List[(String, String)] = jsonConverter(Json.parse(r.text))
    val courseList: List[(String, String)] = dbController.getAllCourses().map(course => (course.id.toString, s"${course.name} - ${Course.getGraduation(course)}"))
    Ok(views.html.main("Antrag", views.html.appreciationAll(aFormAll, uniList, courseList)))
  }

  /**
   * POST: Form appreciation all grades
   *
   * @return
   */
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
              val randomPassword = generateRandomPassword(None)
              println(randomPassword)
              petitionId = dbController.createAppreciation(successForm.firstName, successForm.lastName, successForm.email, successForm.matrNr, successForm.university, Some(successForm.currentPO), Some(successForm.newPO), generateHash(randomPassword, false), successForm.course)

              // Remove existing directory recursive
              if (Files.exists(Paths.get(s"$uploadDir/$petitionId"))) {
                val dir = new Directory(new File(s"$uploadDir/$petitionId"))
                dir.deleteRecursively()
              }

              // Create new directory
              tmpUploadDir = Files.createDirectory(Paths.get(s"$uploadDir/$petitionId"))

              // Upload file in new directory
              file.ref.moveFileTo(Paths.get(s"$tmpUploadDir/Notenkonto.$fileType"), replace = false)

              // Send mail
              sendEmail(
                s"Antrag #${petitionId} eigegangen",
                successForm.email,
                successForm.firstName,
                successForm.lastName,
                s"",
                s"<html><body><p><b>Hallo ${successForm.firstName} ${successForm.lastName},</b><br>Ihr Antrag zur Notenanerkennung ist bei uns eingenagngen " +
                  s"und wird so schnell wie möglich bearbeitet. Den Status Ihres Antrags können Sie mit folgenden Zugangsdaten einsehen:</p>" +
                  s"<p><b>Link:</b> <a href='${rootAddress}${routes.HomeController.showCurrentState(petitionId.toInt)}'>${rootAddress}${routes.HomeController.showCurrentState(petitionId.toInt)}</a><br>" +
                  s"<b>Passwort:</b> <code>${randomPassword}</code></p>" +
                  s"<p><b>Mit freundlichen Grüßen<br>GradeNet</b></p></html><body>")

              // Redirect and show success alert after successfully transfer all form data
              Redirect(routes.HomeController.showCurrentState(petitionId.toInt)).withSession((s"appreciation${petitionId}", petitionId.toString)).flashing("success" -> "Der Antrag wurde erfolgreich eingereicht!")
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

  /**
   * GET: Show anonymized appreciation status
   *
   * @param id
   * @return
   */
  def showCurrentState(id: Int): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    if (sessionExists(s"appreciation${id}")) {
      val appreciationData: Appreciation = dbController.getSingleAppreciation(id)
      val uploadedFiles: List[File] = getListOfFiles(id)
      val stateList: List[Int] = getStateList()
      val moduleList: List[Module] = dbController.getModulesFromAppreciation(id)
      Ok(views.html.main("Status", views.html.adminPanelDetails(appreciationData, uploadedFiles, stateList, moduleList)))
    } else {
      Redirect(routes.HomeController.stateLogin(id))
    }
  }

  /**
   * GET: Admin login page
   *
   * @return
   */
  def adminLogin: Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    if (sessionExists("admin")) {
      Redirect(routes.HomeController.adminPanel())
    } else {
      Ok(views.html.main("Anmeldung", views.html.login(true, 0)))
    }
  }

  /**
   * GET: State login page
   *
   * @return
   */
  def stateLogin(id: Int): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    if (sessionExists(s"appreciation${id}")) {
      Redirect(routes.HomeController.showCurrentState(id))
    } else {
      Ok(views.html.main("Anmeldung", views.html.login(false, id)))
    }
  }

  /**
   * POST: Login user
   *
   * @return
   */
  def login: Action[AnyContent] = Action(parse.anyContent) { implicit request =>
    loginForm.bindFromRequest.fold(
      errorForm => {
        // Redirect and show error alert
        Redirect(routes.HomeController.adminLogin()).flashing("error" -> "Fehlende Angaben!")
      },
      successForm => {
        // Hash password input
        val user: User = dbController.getUser(successForm.username)
        val passInput: String = generateHash(successForm.password, false)
        println("Input Hash: " + passInput)
        println("DB Hash: " + user.password)
        println("Admin: " + user.admin)

        // Validate username and password
        if (user.password == passInput) {
          if (user.admin) {
            println("Logged in successfully!")
            Redirect(routes.HomeController.adminPanel()).withSession(("admin", user.username))
          } else {
            println("No permissions!")
            Redirect(routes.HomeController.adminLogin()).flashing("error" -> "Fehlende Berechtigungen!")
          }
        } else {
          println("Access denied!")
          Redirect(routes.HomeController.adminLogin()).flashing("error" -> "Benutzername oder Passwort falsch!")
        }
      }
    )
  }

  /**
   * POST: Login user
   *
   * @return
   */
  def auth: Action[AnyContent] = Action(parse.anyContent) { implicit request =>
    authForm.bindFromRequest.fold(
      errorForm => {
        // Redirect and show error alert
        Redirect(routes.HomeController.adminLogin()).flashing("error" -> "Fehlende Angaben!")
      },
      successForm => {
        // Hash password input
        val appreciation: Appreciation = dbController.getSingleAppreciation(successForm.appreciationId)
        val passInput: String = generateHash(successForm.password, false)
        println("Input Hash: " + passInput)
        println("DB Hash: " + appreciation.password)

        // Validate username and password
        if (appreciation.password == passInput) {
          println("Logged in successfully!")
          Redirect(routes.HomeController.showCurrentState(successForm.appreciationId)).withSession((s"appreciation${successForm.appreciationId}", appreciation.id.toString))
        } else {
          println("Access denied!")
          Redirect(routes.HomeController.stateLogin(successForm.appreciationId)).flashing("error" -> s"Das Passwort für Antrag #${successForm.appreciationId} ist falsch!")
        }
      }
    )
  }

  /**
   * GET: Logout current logged user
   *
   * @return
   */
  def logout: Action[AnyContent] = Action {
    implicit request: Request[AnyContent] =>
      // Clear session flash and redirect
      Redirect(routes.HomeController.home()).withNewSession.flashing("success" -> "Sie wurden erfolgreich abgemeldet!")
  }

  /**
   * GET: Admin panel
   *
   * @return
   */
  def adminPanel: Action[AnyContent] = Action {
    implicit request: Request[AnyContent] =>
      if (sessionExists("admin")) {
        // Get list of all appreciations and render on template
        val data: List[Appreciation] = dbController.getAllAppreciations()
        Ok(views.html.main("Admin Panel", views.html.adminPanel(data)))
      } else {
        Redirect(routes.HomeController.adminLogin())
      }
  }

  /**
   * GET: Admin panel details
   *
   * @param id
   * @return
   */
  def adminPanelDetails(id: Int): Action[AnyContent] = Action {
    implicit request: Request[AnyContent] =>
      if (sessionExists("admin")) {
        // Get appreciation details and render data on template
        val appreciationData: Appreciation = dbController.getSingleAppreciation(id)
        val uploadedFiles: List[File] = getListOfFiles(id)
        val stateList: List[Int] = getStateList()
        val moduleList: List[Module] = dbController.getModulesFromAppreciation(id)
        Ok(views.html.main("Admin Panel", views.html.adminPanelDetails(appreciationData, uploadedFiles, stateList, moduleList)))
      } else {
        Redirect(routes.HomeController.adminLogin())
      }
  }

  /**
   * POST: Change appreciation state
   *
   * @param id
   * @return
   */
  def adminPanelDetailsChangeState(id: Int): Action[AnyContent] = Action(parse.anyContent) {
    implicit request =>
      if (sessionExists("admin")) {
        stateForm.bindFromRequest.fold(
          errorForm => {
            // Redirect and show error alert
            Redirect(routes.HomeController.adminPanelDetails(id)).flashing("error" -> "Fehler beim Ändern des Status!")
          },
          successForm => {
            // Change state in database
            val appreciationData: Appreciation = dbController.getSingleAppreciation(id)
            dbController.changeAppreciationState(id, successForm.state)

            // Send mail
            sendEmail(
              s"Statusänderungen Antrag #${appreciationData.id}",
              appreciationData.email,
              appreciationData.firstName,
              appreciationData.lastName,
              s"",
              s"<html><body><p><b>Hallo ${appreciationData.firstName} ${appreciationData.lastName},</b><br>" +
                s"""der Status Ihres Antrags #${appreciationData.id} wurde auf "<b>${stateToString(successForm.state)}</b>" geändert!</p>""" +
                s"<p><b>Mit freundlichen Grüßen<br>GradeNet</b></p></html><body>")

            // Redirect after change and show changes
            Redirect(routes.HomeController.adminPanelDetails(id)).flashing("success" ->
              s"""Status von Antrag #${
                id
              } wurde erfolgreich auf "${
                stateToString(successForm.state)
              }" geändert!""")
          }
        )
      } else {
        Redirect(routes.HomeController.adminLogin())
      }
  }

  /**
   * GET: AdminPanel courses
   *
   * @return
   */
  def adminPanelCourses: Action[AnyContent] = Action {
    implicit request: Request[AnyContent] =>
      if (sessionExists("admin")) {
        // Get list of all courses and render on template
        val coursesList: List[Course] = dbController.getAllCourses()
        Ok(views.html.main("Admin Panel", views.html.adminPanelCourses(coursesList)))
      } else {
        Redirect(routes.HomeController.adminLogin())
      }
  }

  /**
   * POST: Create new course
   *
   * @return
   */
  def adminPanelCoursesCreate = Action(parse.multipartFormData) {
    implicit request =>
      courseForm.bindFromRequest.fold(
        errorForm => {
          // Redirect and show error alert
          Redirect(routes.HomeController.adminPanelCourses).flashing("error" -> "Fehlende Angaben! Bitte füllen Sie alle notwendigen Felder aus.")
        },
        successForm => {
          // Create new course in database
          dbController.createCourse(successForm.name, successForm.gradiation, successForm.semester)

          // Redirect and show success alert
          Redirect(routes.HomeController.adminPanelCourses).flashing("success" ->
            s"""Der Studiengang "${
              successForm.name
            }" wurde erfolgreich angelegt!""")
        }
      )
  }

  /**
   * GET: Remove specific course with all included modules
   *
   * @param id
   * @return
   */
  def adminPanelSingleCourseRemove(id: Int): Action[AnyContent] = Action {
    implicit request: Request[AnyContent] =>
      if (sessionExists("admin")) {
        val course: Course = dbController.getCourse(id)
        if (dbController.removeCourse(id) > 0) {
          // Redirect after success
          Redirect(routes.HomeController.adminPanelCourses).flashing("success" ->
            s"""Der Studiengang "${
              course.name
            } - ${
              Course.getGraduation(course)
            }" wurde erfolgreich entfernt!""")
        } else {
          // Redirect and show error alert
          Redirect(routes.HomeController.adminPanelCourses).flashing("error" ->
            s"""Der Studiengang "${
              course.name
            } - ${
              Course.getGraduation(course)
            }" konnte nicht entfernt werden!""")
        }
      } else {
        Redirect(routes.HomeController.adminLogin())
      }
  }

  /**
   * GET: Show single course page
   *
   * @param id
   * @return
   */
  def adminPanelSingleCourse(id: Int): Action[AnyContent] = Action {
    implicit request: Request[AnyContent] =>
      if (sessionExists("admin")) {
        val course: Course = dbController.getCourse(id)
        val filledForm = courseForm.fill(CourseData(course.name, course.graduation, course.semester))
        val courseModuleList = dbController.getAllModules(id)
        Ok(views.html.main("Admin Panel", views.html.adminPanelSingleCourse(id, filledForm, course, courseModuleList)))
      } else {
        Redirect(routes.HomeController.adminLogin())
      }
  }

  /**
   * POST: Edit existing course
   *
   * @param id
   * @return
   */
  def adminPanelSingleCourseEdit(id: Int): Action[AnyContent] = Action {
    implicit request: Request[AnyContent] =>
      if (sessionExists("admin")) {
        val oldCourse: Course = dbController.getCourse(id)
        courseForm.bindFromRequest.fold(
          errorForm => {
            // Redirect and show error alert
            Redirect(routes.HomeController.adminPanelSingleCourse(id)).flashing("error" -> "Fehlende Angaben! Bitte füllen Sie alle notwendigen Felder aus.")
          },
          successForm => {
            // Edit database entry
            dbController.editCourse(Course(id, successForm.name, successForm.gradiation, successForm.semester))

            // Redirect after success
            Redirect(routes.HomeController.adminPanelSingleCourse(id)).flashing("success" ->
              s"""Der Studiengang "${
                oldCourse.name
              }" wurde erfolgreich aktualisiert!""")
          }
        )
      } else {
        Redirect(routes.HomeController.adminLogin())
      }
  }

  /**
   * POST: Create new course
   *
   * @param courseId
   * @return
   */
  def adminPanelSingleModuleCreate(courseId: Int) = Action(parse.multipartFormData) {
    implicit request =>
      moduleForm.bindFromRequest.fold(
        errorForm => {
          // Redirect and show error alert
          Redirect(routes.HomeController.adminPanelSingleCourse(courseId)).flashing("error" -> "Fehlende Angaben! Bitte füllen Sie alle notwendigen Felder aus.")
        },
        successForm => {
          // Create new module in database
          dbController.createModule(successForm.name, successForm.semester, courseId)

          // Redirect and show success alert
          Redirect(routes.HomeController.adminPanelSingleCourse(courseId)).flashing("success" ->
            s"""Das Modul "${
              successForm.name
            }" wurde erfolgreich angelegt!""")
        }
      )
  }

  /**
   * GET: Edit single module
   *
   * @param courseId
   * @param moduleId
   * @return
   */
  def adminPanelSingleModuleEdit(courseId: Int, moduleId: Int): Action[AnyContent] = Action {
    implicit request: Request[AnyContent] =>
      if (sessionExists("admin")) {
        val oldModule: Module = dbController.getModule(moduleId)
        moduleForm.bindFromRequest.fold(
          errorForm => {
            // Redirect and show error alert
            Redirect(routes.HomeController.adminPanelSingleCourse(courseId)).flashing("error" -> "Fehlende Angaben! Bitte füllen Sie alle notwendigen Felder aus.")
          },
          successForm => {
            // Edit database entry
            dbController.editModule(Module(moduleId, successForm.name, None, successForm.semester, courseId))

            // Redirect after success
            Redirect(routes.HomeController.adminPanelSingleCourse(courseId)).flashing("success" ->
              s"""Das Modul "${
                oldModule.name
              }" wurde erfolgreich aktualisiert!""")
          }
        )
      } else {
        Redirect(routes.HomeController.adminLogin())
      }
  }

  /**
   * GET: Remove single module from course
   *
   * @param courseId
   * @param moduleId
   * @return
   */
  def adminPanelSingleModuleRemove(courseId: Int, moduleId: Int): Action[AnyContent] = Action {
    implicit request: Request[AnyContent] =>
      if (sessionExists("admin")) {
        val module: Module = dbController.getModule(moduleId)
        if (dbController.removeModule(moduleId) > 0) {
          // Redirect after success
          Redirect(routes.HomeController.adminPanelSingleCourse(courseId)).flashing("success" ->
            s"""Das Modul "${
              module.name
            }" wurde erfolgreich entfernt!""")
        } else {
          // Redirect and show error alert
          Redirect(routes.HomeController.adminPanelSingleCourse(courseId)).flashing("error" ->
            s"""Das Modul "${
              module.name
            }" konnte nicht entfernt werden!""")
        }
      } else {
        Redirect(routes.HomeController.adminLogin())
      }
  }

  /**
   * GET: Download a specific file
   *
   * @param id
   * @param fileName
   * @return
   */
  def downloadFile(id: Int, fileName: String): Action[AnyContent] = Action {
    implicit request: Request[AnyContent] =>
      if (sessionExists("admin") || sessionExists(s"appreciation${
        id
      }")) {
        implicit val ec = ExecutionContext.global
        val appreciationData: Appreciation = dbController.getSingleAppreciation(id)
        Ok.sendFile(
          content = new java.io.File(s"${uploadDir}/${id}/${fileName}"), fileName = _ => s"Antrag#${id}_${appreciationData.lastName}_${appreciationData.firstName}_${fileName.replace(' ', '_')}", inline = false)
      } else {
        Redirect(routes.HomeController.adminLogin())
      }
  }

  /** *************************
   * * HELPER FUNCTIONS
   * * *************************/

  implicit val moduleWrites = new Writes[Module] {
    def writes(module: Module) = Json.obj(
      "id" -> module.id,
      "name" -> module.name,
      "semester" -> module.semester,
      "course" -> module.course
    )
  }

  def getModulesAsJson(id: Int) = Action { implicit request =>
    import play.api.libs.json._
    val courseModuleList = dbController.getAllModules(id)
    Ok(Json.toJson(courseModuleList))
  }

  /**
   * Convert JSON Array in (value, content) pair for select field
   *
   * @param jsValue
   * @return
   */
  def jsonConverter(jsValue: JsValue): List[(String, String)] = {
    jsValue match {
      case JsArray(jsArray) => jsArray.map(v => (extract(v, "name"), extract(v, "name"))).toList
      case _ => throw new IllegalArgumentException("JSON hat unerwartete Struktur!")
    }
  }

  /**
   * Extract JSON property
   *
   * @param v
   * @param property
   * @return
   */
  def extract(v: JsValue, property: String): String = {
    v match {
      case JsObject(map) => map.get(property) match {
        case Some(JsString(str)) => str
        case _ => throw new IllegalArgumentException("JSON hat unerwartete Struktur!")
      }
      case _ => throw new IllegalArgumentException("JSON hat unerwartete Struktur!")
    }
  }

  /**
   * Return list of all files
   *
   * @param id
   * @return
   */
  def getListOfFiles(id: Int): List[File] = {
    val dir = new File(s"${
      uploadDir
    }/${
      id
    }")
    if (dir.exists && dir.isDirectory) {
      dir.listFiles.filter(_.isFile).toList
    } else {
      List[File]()
    }
  }

  /**
   * Check exist state of session
   *
   * @param sessionName
   * @param request
   * @return
   */
  def sessionExists(sessionName: String)(implicit request: Request[AnyContent]): Boolean = {
    request.session
      .get(sessionName)
      .map {
        _ =>
          return true
      }.getOrElse {
      return false
    }
  }
}
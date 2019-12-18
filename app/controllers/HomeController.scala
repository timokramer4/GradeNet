package controllers

import java.io.File
import java.nio.file.{Files, Path, Paths}

import controllers.Forms.AppreciationForm._
import controllers.Forms.StateForm._
import controllers.Forms.LoginForm._
import javax.inject._
import models.State._
import models.{Student, User}
import play.api.libs.json.{JsArray, JsObject, JsString, JsValue, Json}
import play.api.mvc.{AnyContent, _}
import controllers.Hasher.generateHash

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
  def home() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.main("Startseite", views.html.home()))
  }

  // GET: Appreciation single grades
  def appreciationSingle() = Action { implicit request: Request[AnyContent] =>
    val r = requests.get("http://universities.hipolabs.com/search?country=germany")
    val uniList: List[(String, String)] = jsonConverter(Json.parse(r.text)) // [{}, {}]
    val moduleList: List[(String, String)] = dbController.getModules().map(module => (module.id.toString, module.name))
    Ok(views.html.main("Antrag", views.html.appreciationSingle(aFormSingle, uniList, moduleList)))
  }

  // POST: Form appreciation single grades
  def appreciationSinglePost = Action(parse.multipartFormData) { implicit request =>
    aFormSingle.bindFromRequest.fold(
      errorForm => {
        Redirect(routes.HomeController.appreciationSingle).flashing("error" -> "Fehlende Angaben! Bitte füllen Sie alle notwendigen Felder aus.")
      },
      successForm => {
        println(successForm.modules)

        request.body
          .file("gradeFile")
          .map { file =>
            // Only get the last part of the filename without path
            val filename: Path = Paths.get(file.filename).getFileName
            val fileArray: Array[String] = Paths.get(file.filename).getFileName().toString().split('.')
            val fileType: String = fileArray(fileArray.length - 1)
            var petitionId: Long = 0

            if (fileType == "pdf") {
              // Create upload dir, if not exists
              if (!Files.exists(Paths.get(uploadDir))) {
                Files.createDirectory(Paths.get(uploadDir))
              }

              // Create new appreciation in database
              petitionId = dbController.createAppreciation(successForm.firstName, successForm.lastName, successForm.email, successForm.matrNr, successForm.university)
              dbController.appendModuleToAppreciation(petitionId, successForm.modules)

              // Remove existing directory recursive
              if (Files.exists(Paths.get(s"$uploadDir/$petitionId"))) {
                val dir = new Directory(new File(s"$uploadDir/$petitionId"))
                dir.deleteRecursively()
              }

              // Create new directory
              tmpUploadDir = Files.createDirectory(Paths.get(s"$uploadDir/$petitionId"))

              // Upload file in new directory
              file.ref.moveFileTo(Paths.get(s"$tmpUploadDir/$filename"), replace = false)

              // Redirect and show success alert after successfully transfer all form data
              Redirect(routes.HomeController.appreciationSingle).flashing("success" -> "Der Antrag wurde erfolgreich eingereicht!")
            } else {
              Redirect(routes.HomeController.appreciationSingle).flashing("error" -> "Es sind nur PDF-Dateien als Anhang erlaubt!")
            }
          }.getOrElse {
          // Redirect and show error
          Redirect(routes.HomeController.appreciationSingle).flashing("error" -> "Fehlender Anhang. Ein Antrag ohne Anhang ist nicht möglich!")
        }
      }
    )
  }

  // GET: Appreciation all grades
  def appreciationAll() = Action { implicit request: Request[AnyContent] =>
    val r = requests.get("http://universities.hipolabs.com/search?country=germany")
    val uniList: List[(String, String)] = jsonConverter(Json.parse(r.text))
    Ok(views.html.main("Antrag", views.html.appreciationAll(aFormAll, uniList)))
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
            val filename: Path = Paths.get(file.filename).getFileName
            val fileArray: Array[String] = Paths.get(file.filename).getFileName().toString().split('.')
            val fileType: String = fileArray(fileArray.length - 1)
            var petitionId: Long = 0

            if (fileType == "pdf") {
              // Create upload dir, if not exists
              if (!Files.exists(Paths.get(uploadDir))) {
                Files.createDirectory(Paths.get(uploadDir))
              }

              // Create new appreciation in database
              petitionId = dbController.createAppreciation(successForm.firstName, successForm.lastName, successForm.email, successForm.matrNr, successForm.university)

              // Remove existing directory recursive
              if (Files.exists(Paths.get(s"$uploadDir/$petitionId"))) {
                val dir = new Directory(new File(s"$uploadDir/$petitionId"))
                dir.deleteRecursively()
              }

              // Create new directory
              tmpUploadDir = Files.createDirectory(Paths.get(s"$uploadDir/$petitionId"))

              // Upload file in new directory
              file.ref.moveFileTo(Paths.get(s"$tmpUploadDir/$filename"), replace = false)

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

  // GET: Login page
  def loginPage: Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.main("Anmeldung", views.html.login()))
  }

  // POST: Login user
  def login = Action(parse.anyContent) { implicit request =>
    loginForm.bindFromRequest.fold(
      errorForm => {
        Redirect(routes.HomeController.loginPage()).flashing("error" -> "Fehlende Angaben!")
      },
      successForm => {
        val user: User = dbController.getUser(successForm.username)
        val passInput: String = generateHash(successForm.password)
        println("Input Hash: " + passInput)
        println("DB Hash: " + user.password)
        println(user.admin)
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
      val data: List[Student] = dbController.getAllAppreciations()
      Ok(views.html.main("Admin Panel", views.html.adminPanel(data)))
    } else {
      Redirect(routes.HomeController.loginPage())
    }
  }

  // GET: Admin panel details
  def adminPanelDetails(id: Int): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    if (checkLogin(request)) {
      val appreciationData: Student = dbController.getSingleAppreciation(id)
      val uploadedFiles: List[File] = getListOfFiles(id)
      val stateList: List[Int] = getStateList()
      Ok(views.html.main("Admin Panel", views.html.adminPanelDetails(appreciationData, uploadedFiles, stateList)))
    } else {
      Redirect(routes.HomeController.loginPage())
    }
  }

  // POST: Change appreciation state
  def adminPanelDetailsChangeState(id: Int) = Action(parse.anyContent) { implicit request =>
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

  // GET: Download a specific file
  def downloadFile(id: Int, fileName: String): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    if (checkLogin(request)) {
      implicit val ec = ExecutionContext.global
      Ok.sendFile(
        content = new java.io.File(s"${uploadDir}/${id}/${fileName}"),
        fileName = _ => s"Antrag#${id}_${fileName}",
        inline = false
      )
    } else {
      Redirect(routes.HomeController.loginPage())
    }
  }

  def downloadAllFiles(id: Int): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    if (checkLogin(request)) {
      getListOfFiles(id).foreach { file =>
        println(file.getName())
        downloadFile(id, file.getName())
      }
      Redirect(routes.HomeController.adminPanelDetails(id))
    } else {
      Redirect(routes.HomeController.loginPage())
    }
  }

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
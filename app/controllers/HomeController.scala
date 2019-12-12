package controllers

import java.io.File
import java.nio.file.{Files, Path, Paths}

import controllers.Forms.AppreciationForm._
import controllers.Forms.StateForm._
import controllers.Forms.LoginForm._
import javax.inject._
import models.State._
import models.{Student, User}
import play.api.libs.json.{JsArray, JsValue, Json}
import play.api.mvc.{AnyContent, _}
import controllers.Hasher.generateHash

import scala.concurrent.ExecutionContext
import scala.reflect.io.Directory

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */

@Singleton
class HomeController @Inject()(dbController: DatabaseController, cc: ControllerComponents) extends AbstractController(cc) {

  // GET: Landing page
  def home() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.home())
  }

  // GET: Appreciation single grades
  def appreciationSingle() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.appreciationSingle())
  }

  // GET: Appreciation all grades
  def appreciationAll() = Action { implicit request: Request[AnyContent] =>
    val r = requests.get("http://universities.hipolabs.com/search?country=germany")
    val rawData: JsValue = Json.parse(r.text)
    rawData match {
      case a: JsArray => {
        var unis: List[Map[String, String]] = Nil
        for (i <- 0 to a.value.size - 1) {
          unis = List(Map("id" -> i.toString(), "name" -> a(i).apply("name").as[String])).:::(unis)
        }
        Ok(views.html.appreciationAll(unis))
      }
      case _ => Ok(views.html.home())
    }
  }

  // POST: Form appreciation all grades
  var tmpUploadDir: Path = _
  val uploadDir: String = "uploads"

  def appreciationAllPost = Action(parse.multipartFormData) { implicit request =>
    aForm.bindFromRequest.fold(
      errorForm => {
        Redirect(routes.HomeController.appreciationAll).flashing("error" -> "Fehlende Angaben! Bitte füllen Sie alle notwendigen Felder aus.")
        //        BadRequest(views.html.appreciationAll(errorForm))
      },
      successForm => {
        request.body
          .file("fileChooser")
          .map { file =>
            // Only get the last part of the filename without path
            val filename: Path = Paths.get(file.filename).getFileName
            var petitionId: Long = 0

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
            println("Create dir with ID = " + petitionId)
            tmpUploadDir = Files.createDirectory(Paths.get(s"$uploadDir/$petitionId"))

            // Upload file in new directory
            file.ref.moveFileTo(Paths.get(s"$tmpUploadDir/$filename"), replace = false)

            // Redirect and show success alert after successfully transfer all form data
            Redirect(routes.HomeController.appreciationAll).flashing("success" -> "Der Antrag wurde erfolgreich eingereicht!")
          }
          .getOrElse {
            // Redirect and show error
            Redirect(routes.HomeController.appreciationAll).flashing("error" -> "Fehlender Anhang. Ein Antrag ohne Anhang ist nicht möglich!")
          }
      }
    )
  }

  // GET: Login page
  def loginPage: Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.login())
  }

  // POST: Login user
  def loginUser = Action(parse.anyContent) { implicit request =>
    loginForm.bindFromRequest.fold(
      errorForm => {
        Redirect(routes.HomeController.loginPage()).flashing("error" -> "Fehlende Angaben!")
      },
      successForm => {
        val user: User = dbController.getUser(successForm.username)
        val passInput: String = generateHash(successForm.password)
        println("Input Hash: " + passInput)
        println("DB Hash: " + user.password)
        if (user.password == passInput) {
          println("Angemeldet!")
          Redirect(routes.HomeController.adminPanel()).withSession("connected" -> user.username)
        } else {
          println("Zugang verweigert!")
          Redirect(routes.HomeController.loginPage()).flashing("error" -> "Benutzername oder Passwort falsch!")
        }
      }
    )
  }

  // GET: Logout current logged user
  def logout: Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Redirect(routes.HomeController.home()).withNewSession/*.flashing("success" -> "Sie wurden erfolgreich abgemeldet!")*/
  }

  // GET: Admin panel
  def adminPanel: Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    if (checkLogin(request)) {
      val data: List[Student] = dbController.getAllAppreciations()
      Ok(views.html.adminPanel(data))
    } else {
      Redirect(routes.HomeController.loginPage())
    }
  }

  // GET: Admin panel details
  def adminPanelDetails(id: Int): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    val appreciationData: Student = dbController.getSingleAppreciation(id)
    val uploadedFiles: List[File] = getListOfFiles(s"$uploadDir/$id")
    val stateList: List[Int] = getStateList()
    Ok(views.html.adminPanelDetails(appreciationData, uploadedFiles, stateList))
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

  // Return list of all files
  def getListOfFiles(path: String): List[File] = {
    val dir = new File(path)
    if (dir.exists && dir.isDirectory) {
      dir.listFiles.filter(_.isFile).toList
    } else {
      List[File]()
    }
  }

  // GET: Download a specific file
  def downloadFile(id: Int, fileName: String): Action[AnyContent] = Action {
    implicit val ec = ExecutionContext.global
    Ok.sendFile(
      content = new java.io.File(s"${uploadDir}/${id}/${fileName}"),
      fileName = _ => s"Antrag#${id}_${fileName}",
      inline = true
    )
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

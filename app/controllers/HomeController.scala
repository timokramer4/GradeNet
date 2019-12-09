package controllers

import java.io.File
import java.nio.file.{Files, Path, Paths}
import controllers.AppreciationForm._
import javax.inject._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import scala.reflect.io.Directory

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */

case class UserData(firstName: String, lastName: String, email: String, matrNr: Int, university: Int)

@Singleton
class HomeController @Inject()(dbController: DatabaseController, cc: ControllerComponents) extends AbstractController(cc) {

  // GET: Landing page
  def home() = Action { implicit request: Request[AnyContent] =>
    val r = requests.get("http://universities.hipolabs.com/search?country=germany")
    print("JSONString: " + r.text) // [{}, {}]
    val jsonList: List[JsValue] = Json.parse(r.text).as[List[JsValue]]
    val universities: List[JsValue] = jsonList.filter(json => (json \ "name").as[Boolean])
    print(universities)
    Ok(r.text)
    //Ok(views.html.home())
  }

  // GET: Appreciation single grades
  def appreciationSingle() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.appreciationSingle())
  }

  // GET: Appreciation all grades
  def appreciationAll() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.appreciationAll())
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

  // GET: Admin panel
  def adminPanel: Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    val data: List[Map[String, Any]] = dbController.getAllAppreciations()
    /*data.foreach(appreciation =>
      for ((k, v) <- appreciation) {
        printf("key: %s, value: %s\n", k, v)
      }
    )*/
    Ok(views.html.adminPanel(data))
  }

  // GET: Admin panel details
  def adminPanelDetails(id: Int): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    val appreciationData: Map[String, Any] = dbController.getSingleAppreciation(id)
    val uploadedFiles: List[File] = getListOfFiles(s"$uploadDir/$id")
    Ok(views.html.adminPanelDetails(appreciationData, uploadedFiles))
  }

  def getListOfFiles(path: String): List[File] = {
    val dir = new File(path)
    if (dir.exists && dir.isDirectory) {
      dir.listFiles.filter(_.isFile).toList
    } else {
      List[File]()
    }
  }
}

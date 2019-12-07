package controllers

import java.nio.file.{Files, Path, Paths}
import controllers.AppreciationForm._
import javax.inject._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._

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
            var folderId: Long = 0

            // Create upload dir, if not exists
            if (!Files.exists(Paths.get(uploadDir))) {
              Files.createDirectory(Paths.get(uploadDir))
            }

            // Create new appreciation
            folderId = dbController.createAppreciation(successForm.firstName, successForm.lastName, successForm.email, successForm.matrNr, successForm.university)

            // Create new appreciation dir, if not already exists
            if (!Files.exists(Paths.get(s"$uploadDir/$folderId"))) {
              tmpUploadDir = Files.createDirectory(Paths.get(s"$uploadDir/$folderId"))

              // Upload file in new directory
              file.ref.moveFileTo(Paths.get(s"$tmpUploadDir/$filename"), replace = false)

              // Redirect and show success alert after successfully transfer all form data
              Redirect(routes.HomeController.appreciationAll).flashing("success" -> "Der Antrag wurde erfolgreich eingereicht!")
            } else {
              // Remove wrong appreciation
              dbController.removeAppreciation(folderId)
              // Redirect and show error
              Redirect(routes.HomeController.appreciationAll).flashing("error" -> "Fehler beim Hochladen des Anhangs! Das Verzeichnis existiert bereits. Bitte versuchen Sie es erneut!")
            }
          }
          .getOrElse {
            // Redirect and show error
            Redirect(routes.HomeController.appreciationAll).flashing("error" -> "Fehlender Anhang. Ein Antrag ohne Anhang ist nicht möglich!")
          }
      }
    )
  }
}

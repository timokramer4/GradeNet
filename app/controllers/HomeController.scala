package controllers

import java.nio.file.{Files, Path, Paths}
import javax.inject._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import AppreciationForm._
import play.api.data.FormError

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */

case class UserData(firstName: String, lastName: String, email: String, matrNr: Int, university: Int)

@Singleton
class HomeController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */

  def home() = Action { implicit request: Request[AnyContent] =>
    val r = requests.get("http://universities.hipolabs.com/search?country=germany")
    print("JSONString: " + r.text) // [{}, {}]
    val jsonList: List[JsValue] = Json.parse(r.text).as[List[JsValue]]
    val universities: List[JsValue] = jsonList.filter(json => (json \ "name").as[Boolean])
    print(universities)
    Ok(r.text)
    //Ok(views.html.home())
  }

  def appreciationSingle() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.appreciationSingle())
  }

  def appreciationAll() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.appreciationAll())
  }

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
            val r = scala.util.Random
            val folderId: Int = r.nextInt(100) // must be read dynamically from database last appreciation id

            // Create upload dir, if not exists
            if (!Files.exists(Paths.get(uploadDir))) {
              Files.createDirectory(Paths.get(uploadDir))
            }

            // Create new appreciation dir, if not already exists
            if (!Files.exists(Paths.get(s"$uploadDir/$folderId"))) {
              tmpUploadDir = Files.createDirectory(Paths.get(s"$uploadDir/$folderId"))

              // Upload file in new directory
              file.ref.moveFileTo(Paths.get(s"$tmpUploadDir/$filename"), replace = false)

              // TODO: Read other input fields and push on database
              println(successForm.firstName)
              println(successForm.lastName)
              println(successForm.email)
              println(successForm.matrNr)
              println(successForm.university)

              // Redirect after successfully transfer all form data
              Redirect(routes.HomeController.appreciationAll).flashing("success" -> "Der Antrag wurde erfolgreich eingereicht!")
            } else {
              Redirect(routes.HomeController.appreciationAll).flashing("error" -> "Fehler beim Hochladen des Anhangs! Das Verzeichnis existiert bereits. Bitte versuchen Sie es erneut!")
            }
          }
          .getOrElse {
            Redirect(routes.HomeController.appreciationAll).flashing("error" -> "Fehlender Anhang. Ein Antrag ohne Anhang ist nicht möglich!")
          }
      }
    )
  }

  def convert(text: String, messages: Seq[FormError]): String =
    """<div class="alert" role="alert">""" + text + formatMessages(messages) + "</div>"

  private def formatMessages(messages: Seq[FormError]): String = {
    if (messages.size > 0) {
      "<ul>" + messages.foldLeft("")((res, message) => res + "<li>" + message.message + "</li>") + "</ul>"
    } else ""
  }
}

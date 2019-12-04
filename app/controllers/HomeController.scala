package controllers

import javax.inject._
import play.api._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
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
}

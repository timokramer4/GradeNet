package controllers.Forms

import play.api.data.Form
import play.api.data.Forms._

object LoginForm {
  case class Data(username: String, password: String)

  val loginForm = Form(
    mapping(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText
    )(Data.apply)(Data.unapply)
  )
}

package controllers.Forms

import play.api.data.Form
import play.api.data.Forms._

object LoginForm {

  case class adminData(username: String, password: String)

  case class userData(appreciationId: Int, password: String)

  val loginForm = Form(
    mapping(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText
    )(adminData.apply)(adminData.unapply)
  )

  val authForm = Form(
    mapping(
      "appreciationId" -> number,
      "password" -> nonEmptyText
    )(userData.apply)(userData.unapply)
  )
}

package controllers

object AppreciationForm {
  import play.api.data.Forms._
  import play.api.data.Form

  case class Data(firstName: String, lastName: String, email: String, matrNr: Int, university: Int, fileChooser: Option[String])

  val aForm = Form(
    mapping(
      "firstName" -> nonEmptyText,
      "lastName" -> nonEmptyText,
      "email" -> email,
      "matrNr" -> number(min=100000, max=999999),
      "university" -> number,
      "fileChooser" -> optional(text)
    )(Data.apply)(Data.unapply)
  )
}

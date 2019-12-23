package controllers.Forms

object CourseForm {

  import play.api.data.Form
  import play.api.data.Forms._

  case class Data(
                     name: String,
                     gradiation: Int,
                     semester: Int
                   )

  val courseForm = Form(
    mapping(
      "name" -> nonEmptyText,
      "graduation" -> number(min = 0, max = 1),
      "semester" -> number(min = 3, max = 7)
    )(Data.apply)(Data.unapply)
  )
}
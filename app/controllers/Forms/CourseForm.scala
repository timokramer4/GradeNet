package controllers.Forms

import java.util.Calendar

object CourseForm {

  import play.api.data.Form
  import play.api.data.Forms._

  case class CourseData(
                     name: String,
                     po: Int,
                     gradiation: Int,
                     semester: Int
                   )

  val courseForm = Form(
    mapping(
      "name" -> nonEmptyText,
      "po" -> number(Calendar.getInstance.get(Calendar.YEAR) - 10, Calendar.getInstance.get(Calendar.YEAR)),
      "graduation" -> number(min = 0, max = 1),
      "semester" -> number(min = 3, max = 7)
    )(CourseData.apply)(CourseData.unapply)
  )
}
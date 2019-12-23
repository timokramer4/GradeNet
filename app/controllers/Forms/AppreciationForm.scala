package controllers.Forms

object AppreciationForm {

  import play.api.data.Form
  import play.api.data.Forms._

  case class All(
                  firstName: String,
                  lastName: String,
                  email: String,
                  matrNr: Int,
                  course: Int,
                  university: String
                )

  case class Single(
                     firstName: String,
                     lastName: String,
                     email: String,
                     matrNr: Int,
                     university: String,
                     course: Int,
                     modules: List[Int]
                   )

  val aFormSingle = Form(
    mapping(
      "firstName" -> nonEmptyText,
      "lastName" -> nonEmptyText,
      "email" -> email,
      "matrNr" -> number(min = 100000, max = 999999),
      "university" -> nonEmptyText,
      "course" -> number,
      "modules" -> list(number)
    )(Single.apply)(Single.unapply)
  )

  val aFormAll = Form(
    mapping(
      "firstName" -> nonEmptyText,
      "lastName" -> nonEmptyText,
      "email" -> email,
      "matrNr" -> number(min = 100000, max = 999999),
      "course" -> number,
      "university" -> nonEmptyText
    )(All.apply)(All.unapply)
  )
}
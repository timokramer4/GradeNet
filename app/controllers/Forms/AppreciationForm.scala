package controllers.Forms

object AppreciationForm {

  import play.api.data.Form
  import play.api.data.Forms._

  case class All(
                   firstName: String,
                   lastName: String,
                   email: String,
                   matrNr: Int,
                   university: String,
                   gradeFile: Option[String]
                 )

  case class Single(
                   firstName: String,
                   lastName: String,
                   email: String,
                   matrNr: Int,
                   university: String,
                   gradeFile: Option[String],
                   modules: Seq[Module]
                 )

  case class Module(
                     name: Int,
                     file: Option[String]
                   )

  val aFormSingle = Form(
    mapping(
      "firstName" -> nonEmptyText,
      "lastName" -> nonEmptyText,
      "email" -> email,
      "matrNr" -> number(min = 100000, max = 999999),
      "university" -> nonEmptyText,
      "gradeFile" -> optional(text),
      "modules" -> seq(
        mapping(
          "moduleName" -> number,
          "moduleDescription" -> optional(text)
        )(Module.apply)(Module.unapply)
      )
    )(Single.apply)(Single.unapply)
  )

  val aFormAll = Form(
    mapping(
      "firstName" -> nonEmptyText,
      "lastName" -> nonEmptyText,
      "email" -> email,
      "matrNr" -> number(min = 100000, max = 999999),
      "university" -> nonEmptyText,
      "gradeFile" -> optional(text)
    )(All.apply)(All.unapply)
  )
}

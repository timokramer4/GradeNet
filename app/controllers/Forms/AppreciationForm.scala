package controllers.Forms

object AppreciationForm {

  import play.api.data.Form
  import play.api.data.Forms._

  case class All(
                   firstName: String,
                   lastName: String,
                   email: String,
                   matrNr: Int,
                   university: Int,
                   fileChooser: Option[String]
                 )

  case class Single(
                   firstName: String,
                   lastName: String,
                   email: String,
                   matrNr: Int,
                   university: Int,
                   modules: Seq[Module]
                 )

  case class Module(
                     name: Int,
                     description: String,
                     file: Option[String]
                   )

  val aFormSingle = Form(
    mapping(
      "firstName" -> nonEmptyText,
      "lastName" -> nonEmptyText,
      "email" -> email,
      "matrNr" -> number(min = 100000, max = 999999),
      "university" -> number,
      "modules" -> seq(
        mapping(
          "moduleName" -> number,
          "moduleDescription" -> nonEmptyText,
          "moduleFile" -> optional(text)
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
      "university" -> number,
      "fileChooser" -> optional(text)
    )(All.apply)(All.unapply)
  )
}

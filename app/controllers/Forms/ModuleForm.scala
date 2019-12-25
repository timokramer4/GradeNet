package controllers.Forms

object ModuleForm {

  import play.api.data.Form
  import play.api.data.Forms._

  case class ModuleData(
                     name: String,
                     semester: Int
                   )

  val moduleForm = Form(
    mapping(
      "moduleName" -> nonEmptyText,
      "moduleSemester" -> number(min = 1, max = 7)
    )(ModuleData.apply)(ModuleData.unapply)
  )
}
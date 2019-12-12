package controllers.Forms

object StateForm {
  import play.api.data.Form
  import play.api.data.Forms._

  case class Data(state: Int)

  val stateForm = Form(
    mapping(
      "state" -> number
    )(Data.apply)(Data.unapply)
  )
}

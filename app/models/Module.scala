package models

case class Module(id: Int, name: String, appreciationName: Option[String], semester: Int, course: Int)

object Module {
  def getString(module: Module, key: String): String = {
    key match {
      case "id" => module.id.toString()
      case "name" => module.name
      case "appreciationName" => module.appreciationName.getOrElse("")
      case "semester" => module.semester.toString()
      case _ => "\"" + key + "\" not found!";
    }
  }

  def getId(module: Module): Int =
    module.id
}

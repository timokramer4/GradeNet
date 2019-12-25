package models

case class Module(id: Int, name: String, semester: Int, course: Int)

object Module {
  def getString(module: Module, key: String): String = {
    key match {
      case "id" => module.id.toString()
      case "name" => module.name.toString()
      case "semester" => module.semester.toString()
      case _ => "\"" + key + "\" not found!";
    }
  }

  def getId(module: Module): Int =
    module.id
}

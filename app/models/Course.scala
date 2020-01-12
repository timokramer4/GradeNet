package models

case class Course(id: Int, name: String, po: Int, graduation: Int, semester: Int)

object Course {
  def getGraduation(course: Any): String = {
    course match {
      case c: Int => {
        c match {
          case 0 => "Bachelor"
          case 1 => "Master"
          case _ => "n.A."
        }
      }
      case i: String => {
        i match {
          case "0" => "Bachelor"
          case "1" => "Master"
          case _ => "n.A."
        }
      }
      case _ => throw new IllegalArgumentException("Falscher Datentyp!")
    }
  }
}

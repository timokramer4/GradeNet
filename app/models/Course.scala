package models

case class Course(id: Int, name: String, graduation: Int, semester: Int)

object Course {
  def getString(course: Course, key: String): String = {
    key match {
      case "id" => course.id.toString()
      case "name" => course.name.toString()
      case "graduation" => course.graduation.toString()
      case "semester" => course.semester.toString()
      case _ => "\"" + key + "\" not found!";
    }
  }

  def getId(course: Course): Int =
    course.id

  def getGraduation(course: Any): String = {
    course match {
      case c: Course => {
        c.graduation match {
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

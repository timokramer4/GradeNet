package models

import models.State.stateToString

case class Appreciation(id: Int, firstName: String, lastName: String, matrNr: Int, email: String, university: String, currentPO: Int, newPO: Int, password: String, state: State, course: Option[String])

object Appreciation {
  def getString(appreciation: Appreciation, key: String): String = {
    key match {
      case "id" => appreciation.id.toString
      case "firstName" => appreciation.firstName
      case "lastName" => appreciation.lastName
      case "matrNr" => appreciation.matrNr.toString
      case "email" => appreciation.email
      case "university" => appreciation.university
      case "course" => appreciation.course.getOrElse("").toString
      case "currentPO" => appreciation.currentPO.toString
      case "newPO" => appreciation.newPO.toString
      case "password" => appreciation.password
      case "state" => {
        stateToString(appreciation.state)
      }
      case _ => "\"" + key + "\" not found!";
    }
  }

  def getState(student: Appreciation): State =
    student.state

  def getId(student: Appreciation): Int =
    student.id
}

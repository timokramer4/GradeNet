package models

import models.State.stateToString

case class Appreciation(id: Int, firstName: String, lastName: String, matrNr: Int, email: String, university: String, currentPO: Int, newPO: Int, password: String, state: State)

object Appreciation {
  def getString(student: Appreciation, key: String): String = {
    key match {
      case "id" => student.id.toString
      case "firstName" => student.firstName
      case "lastName" => student.lastName
      case "matrNr" => student.matrNr.toString
      case "email" => student.email
      case "university" => student.university
      case "currentPO" => student.currentPO.toString
      case "newPO" => student.newPO.toString
      case "password" => student.password
      case "state" => {
        stateToString(student.state)
      }
      case _ => "\"" + key + "\" not found!";
    }
  }

  def getState(student: Appreciation): State =
    student.state

  def getId(student: Appreciation): Int =
    student.id
}

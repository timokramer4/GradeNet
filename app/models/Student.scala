package models

import models.State.stateToString

case class Student(id: Int, firstName: String, lastName: String, matrNr: Int, email: String, university: Int, state: State)

object Student {
  def getString(student: Student, key: String): String = {
    key match {
      case "id" => student.id.toString()
      case "firstName" => student.firstName
      case "lastName" => student.lastName
      case "matrNr" => student.matrNr.toString()
      case "email" => student.email
      case "university" => student.university.toString()
      case "state" => {
        stateToString(student.state)
      }
      case _ => "\"" + key + "\" not found!";
    }
  }

  def getState(student: Student): State =
    student.state

  def getId(student: Student): Int =
    student.id
}

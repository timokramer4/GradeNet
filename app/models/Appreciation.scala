package models

import models.State.stateToString

case class Appreciation(id: Int, firstName: String, lastName: String, matrNr: Int, email: String, university: String, currentPO: Int, newPO: Int, password: String, state: State, course: Option[String])

object Appreciation {
  def getString(appreciation: Appreciation, key: String): String = {
    key match {
      case "state" => {
        stateToString(appreciation.state)
      }
      case _ => "\"" + key + "\" not found!";
    }
  }
}

package models

sealed trait State

case object Open extends State
case object Processing extends State
case object Meeting extends State
case object Closed extends State

object State {
  def intToState(intState: Int): State =
    intState match {
      case 0 => Open
      case 1 => Processing
      case 2 => Meeting
      case 3 => Closed
    }

  def stateToString(state: State): String =
    state match {
      case Open => "Offen"
      case Processing => "In Bearbeitung"
      case Meeting => "Terminvereinbarung"
      case Closed => "Abgeschlossen"
      case _ => ""
    }

  def stateColor(state: State): String = {
    state match {
      case Processing => "table-warning"
      case Meeting => "table-danger"
      case Closed => "table-success"
      case _ => ""
    }
  }
}

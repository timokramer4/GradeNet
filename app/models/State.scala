package models

sealed trait State

case object Open extends State

case object Processing extends State

case object Meeting extends State

case object Closed extends State

object State {
  val openStr: String = "Offen"
  val processingStr: String = "In Bearbeitung"
  val meetingStr: String = "Terminvereinbarung"
  val closedStr: String = "Abgeschlossen"
  val noneStr: String = "n.A."

  def stateToString(data: Any): String =
    data match {
      case s: State => {
        s match {
          case Open => return openStr
          case Processing => return processingStr
          case Meeting => return meetingStr
          case Closed => return closedStr
          case _ => noneStr
        }
      }
      case i: Int => {
        i match {
          case 0 => return openStr
          case 1 => return processingStr
          case 2 => return meetingStr
          case 3 => return closedStr
          case _ => noneStr
        }
      }
    }

  def switchStateInt(data: Any): Any = {
    data match {
      case s: State => {
        s match {
          case Open => return 0
          case Processing => return 1
          case Meeting => return 2
          case Closed => return 3
        }
      }
      case i: Int => {
        i match {
          case 0 => return Open
          case 1 => return Processing
          case 2 => return Meeting
          case 3 => return Closed
        }
      }
    }
  }

  def stateColor(state: State, contentType: String): String = {
    state match {
      case Processing => s"${contentType}-warning"
      case Meeting => s"${contentType}-danger"
      case Closed => s"${contentType}-success"
      case _ => ""
    }
  }

  def getStateList(): List[Int] = {
    var stateList: List[Int] = Nil
    for (i <- 0 to 3) {
      stateList = List(i).:::(stateList)
    }
    return stateList
  }
}

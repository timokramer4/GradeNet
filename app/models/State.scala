package models

import play.twirl.api.Html

sealed trait State

case object Open extends State

case object Processing extends State

case object Meeting extends State

case object MissingInfos extends State

case object Closed extends State

object State {
  val openStr: String = "Offen"
  val processingStr: String = "In Bearbeitung"
  val meetingStr: String = "Terminvereinbarung"
  val missingInfos: String = "Fehlende Informationen"
  val closedStr: String = "Abgeschlossen"
  val noneStr: String = "n.A."

  def stateToString(data: Any): String =
    data match {
      case s: State => {
        s match {
          case Open => return openStr
          case Processing => return processingStr
          case Meeting => return meetingStr
          case MissingInfos => return missingInfos
          case Closed => return closedStr
          case _ => noneStr
        }
      }
      case i: Int => {
        i match {
          case 0 => return openStr
          case 1 => return processingStr
          case 2 => return meetingStr
          case 3 => return missingInfos
          case 4 => return closedStr
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
          case MissingInfos => return 3
          case Closed => return 4
        }
      }
      case i: Int => {
        i match {
          case 0 => return Open
          case 1 => return Processing
          case 2 => return Meeting
          case 3 => return MissingInfos
          case 4 => return Closed
        }
      }
    }
  }

  def stateColor(state: State, contentType: String): String = {
    state match {
      case Processing => s"${contentType}-warning"
      case Meeting => s"${contentType}-danger"
      case MissingInfos => s"${contentType}-danger"
      case Closed => s"${contentType}-success"
      case _ => {
        contentType match {
          case "alert" => s"${contentType}-info"
          case _ => ""
        }
      }
    }
  }

  def getStateList(): List[Int] = {
    var stateList: List[Int] = Nil
    for (i <- 0 to 3) {
      stateList = List(i).:::(stateList)
    }
    return stateList
  }

  def getDescription(appreciation: Appreciation): Html = {
    appreciation.state match {
      case Open => Html.apply("Ihr Antrag ist bei uns <strong>eingegangen</strong> und wird so schnell wie möglich bearbeitet!")
      case Processing => Html.apply("Ihr Antrag <strong>wird derzeit bearbeitet</strong>! Bitte haben Sie noch einen Moment Geduld.")
      case Meeting => Html.apply("Um den Auftrag abschließen zu können, muss zuvor ein persönliches Gespräch erfolgen. Bitte wenden Sie " +
        s"sich für die Terminvereinbarung mit der o.g. Antragsnummer (<a href='/state/${appreciation.id}'>#${appreciation.id}</a>) an die Email-Adresse " +
        s"<a href='mailto:info@wasauchimmer.de'>info@wasauchimmer.de</a>.")
      case MissingInfos => Html.apply("Um den Auftrag abschließen zu können, müssen die von Ihnen zur Verfügung gestellten Angaben überarbeitet werden.")
      case Closed => Html.apply(s"Ihr Auftrag wurde <strong>erfolgreich</strong> und ohne Probleme <strong>abgeschlossen</strong>. Die Noten wurden in Ihrem Notenkonto hinzugefügt.")
      case _ => Html.apply("")
    }
  }
}

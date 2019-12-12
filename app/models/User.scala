package models

case class User(username: String, password: String, admin: Boolean)

object User {
  def getUsername(user: User): String =
    user.username

  def getPassword(user: User): String =
    user.password

  def isAdmin(user: User): Boolean =
    user.admin
}

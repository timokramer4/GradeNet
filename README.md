# GradeNet

GradeNet is a tool for online recognition of grades when changing the study program or the currently used examination regulations. The tool was developed in the *programming language **Scala*** together with the *framework **Play***. A database is used to store requests and their associated data. There should be an admin area, through which one can manage the appreciations of the students and give them a status that informs about the current status of the appreciation.

## How to install and run?
To successfully run the Scala application, you first need the ***Scala Build Tool*** (*SBT*), which you can download for all platforms at the following address:
https://www.scala-sbt.org/download.html

After successful installation, start the respective development environment of your choice. We're using [IntelliJ](https://www.jetbrains.com/idea/download/index.html#section=windows) from NetbRains for this tutorial. If you pulled the most current version of the application over the familiar Git commands, you can start integrating with IntelliJ. 

![Welcome page IntelliJ](https://i.ibb.co/V0F7WHm/img01.png)
To do this, **start the program** and, before you start importing the project, call up the **plugin settings** at the bottom right by navigating in `Configure> Plugins`. 

![Scala plugin](https://i.ibb.co/zJZLGKT/img02.png)
It should open the following window, here you are looking for the plugin "**Scala**" and **install this**. After successful installation, it may be **necessary to restart** the development environment. After doing this, please start the IDE again and select on the start page under "**Import**" the directory path where you saved the project.

This completes the integration of the project. The project can now be compiled in the terminal with the command `sbt compile` and executed with command `sbt run`. Alternatively, a run configuration can be created in the IntelliJ development environment. To do this, a new configuration must first be created under `Run>Edit Configurations`. The pre-made template "Play 2 App" can be used here.

## Prepare the database
The application uses a database, here is the choice between a **[MySQL](https://www.mysql.com/de/)** and a **[PostgreSQL](https://www.postgresql.org/)** database. Standard is a PostgreSQL database. If you prefer to use a MySQL database instead, please read the section "[Switch database or change database access](#switch-database-or-change-database-access)" below.

Both databases must have at least one database named `Gradenet`. This must be accessible with the following user access, which also has **full read and write access** to the database:

| **Database** | **Username** | **Password** |
| ------ | ------ | ------ |
| **MySQL** | mysql | mysql |
| **PostgreSQL** | postgres | postgres |

### Switch database or change database access
If you want to switch between MySQL and PostgreSQL database, you can do this in `conf/application.conf` in the `db.default` section. Comment out the unused database! Also changing the access data happens there. To change the username and password you only have to change the variables `default.username` and `default.password`.

```
db {
    [...]
    default.driver = org.postgresql.Driver
    default.url = "jdbc:postgresql://localhost:5432/gradenet"
    default.username = ""
    default.password = ""
    [...]
}
```
## Mail server settings and email notifications

To activate the integrated mail notification, the access data of the mail provider to be used must be changed. The change can also be made in the `conf/application.conf`, navigate to the end of the file in the `player.mail` area. This looks as follows and can be edited accordingly (the following section has been reduced to the essential parts for a successful mail transfer):

```
play.mailer {
  host = "YOUR_MAIL_SERVER_URL"
  port = YOUR_SMTP_PORT
  ssl = yes
  tls = yes
  tlsRequired = yes
  user = "YOUR_SMTP_USERNAME"
  password = "YOUR_SMTP_PASSWORD"
}
```
You'll receive the data necessary for this configuration through your mail provider and the password you set.
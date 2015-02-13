package chapter03

import java.sql.Timestamp
import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC

object ValueClassesExample extends App {

  trait Profile {
    val profile: scala.slick.driver.JdbcProfile
  }

  object PKs {
    case class MessagePK(value: Long) extends AnyVal
    case class UserPK(value: Long) extends AnyVal
  }

  trait Tables {
    this: Profile =>

    import profile.simple._
    import PKs._

    implicit val jodaDateTimeType =
      MappedColumnType.base[DateTime, Timestamp](
        dt => new Timestamp(dt.getMillis),
        ts => new DateTime(ts.getTime, UTC))

    implicit val messagePKMapper = MappedColumnType.base[MessagePK, Long](_.value, MessagePK(_))

    implicit val userPKMapper    = MappedColumnType.base[UserPK, Long](_.value, UserPK(_))


    case class User(name: String, id: UserPK = UserPK(0L))

    class UserTable(tag: Tag) extends Table[User](tag, "user") {
      def id   = column[UserPK]("id", O.PrimaryKey, O.AutoInc)
      def name = column[String]("name")

      def * = (name, id) <> (User.tupled, User.unapply)
    }

    lazy val users = TableQuery[UserTable]
    lazy val insertUser = users returning users.map(_.id)

    case class Message(
        senderId: UserPK,
        content:  String,
        ts:       DateTime,
        id:       MessagePK = MessagePK(0L))

    class MessageTable(tag: Tag) extends Table[Message](tag, "message") {
      def id       = column[MessagePK]("id", O.PrimaryKey, O.AutoInc)
      def senderId = column[UserPK]("sender")
      def content  = column[String]("content")
      def ts       = column[DateTime]("ts")

      def * = (senderId, content, ts, id) <> (Message.tupled, Message.unapply)

      def sender = foreignKey("sender_fk", senderId, users)(_.id, onDelete=ForeignKeyAction.Cascade)
    }

    lazy val messages = TableQuery[MessageTable]
  }


  class Schema(val profile: scala.slick.driver.JdbcProfile) extends Tables with Profile

  val schema = new Schema(scala.slick.driver.H2Driver)

  import schema._, profile.simple._

  def db = Database.forURL("jdbc:h2:mem:chapter03", driver = "org.h2.Driver")

  db.withSession {
    implicit session =>

      (messages.ddl ++ users.ddl).create

      // Users:
      val halId  = insertUser += User("HAL")
      val daveId = insertUser += User("Dave")

      // Insert the conversation, which took place in Feb, 2001:
      val start = new DateTime(2001, 2, 17, 10, 22, 50)

      messages ++= Seq(
        Message(daveId, "Hello, HAL. Do you read me, HAL?", start),
        Message(halId,  "Affirmative, Dave. I read you.", start plusSeconds 2),
        Message(daveId, "Open the pod bay doors, HAL.", start plusSeconds 4),
        Message(halId,  "I'm sorry, Dave. I'm afraid I can't do that.", start plusSeconds 6))

        println(
          users.list
        )
  }

}
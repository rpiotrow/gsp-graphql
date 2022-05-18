// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package twojoin

import cats.effect.Sync
import doobie.Transactor
import doobie.postgres.implicits._
import doobie.util.meta.Meta
import edu.gemini.grackle.doobie._
import edu.gemini.grackle.syntax._

import java.time.ZonedDateTime

trait TwoJoinData[F[_]] extends DoobieMapping[F] {

  object student extends TableDef("students") {
    val id = col("id", Meta[String])
    val firstName = col("first_name", Meta[String])
    val lastName = col("last_name", Meta[String])
  }

  object `class` extends TableDef("classes") {
    val id = col("id", Meta[String])
    val name = col("name", Meta[String])
  }

  object enrollment extends TableDef("enrollments") {
    val studentId = col("student_id", Meta[String])
    val classId = col("class_id", Meta[String])
    val startDate = col("start_date", Meta[ZonedDateTime])
  }

  val schema =
    schema"""
      type Query {
        student: [Student!]!
      }

      scalar DateTime

      type Student {
        firstName: String!
        lastName: String!
        class: [Class]!
      }
      type Class {
        name: String!
        enrollmentDate: [DateTime!]
      }
    """

  val QueryType = schema.ref("Query")
  val DateTimeType = schema.ref("DateTime")
  val StudentType = schema.ref("Student")
  val ClassType = schema.ref("Class")

  val typeMappings =
    List(
      ObjectMapping(
        tpe = QueryType,
        fieldMappings =
          List(
            SqlRoot("student")
          )
      ),
      ObjectMapping(
        tpe = StudentType,
        fieldMappings =
          List(
            SqlField("id", student.id, key = true, hidden = true),
            SqlField("firstName", student.firstName),
            SqlField("lastName", student.lastName),
            SqlObject("class", Join(student.id, enrollment.studentId), Join(enrollment.classId, `class`.id))
          )
      ),
      ObjectMapping(
        tpe = ClassType,
        fieldMappings =
          List(
            SqlField("id", `class`.id, key = true, hidden = true),
            SqlField("name", `class`.name),
            SqlField("enrollmentDate", enrollment.startDate)
          )
      ),
      LeafMapping[ZonedDateTime](DateTimeType),
    )
}

object TwoJoinData extends DoobieMappingCompanion {
  def mkMapping[F[_]: Sync](transactor: Transactor[F], monitor: DoobieMonitor[F]): TwoJoinData[F] =
    new DoobieMapping(transactor, monitor) with TwoJoinData[F]
}

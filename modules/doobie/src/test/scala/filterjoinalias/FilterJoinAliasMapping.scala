// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package filterjoinalias

import cats.effect.Sync
import cats.implicits._
import doobie.Transactor
import doobie.util.meta.Meta
import edu.gemini.grackle._
import doobie._
import syntax._
import Path._
import Predicate.{Const, Eql}
import Query.{Binding, Filter, Select, Unique}
import QueryCompiler.SelectElaborator
import QueryInterpreter.mkErrorResult
import Value.{AbsentValue, NullValue, ObjectValue, StringValue}

trait FilterJoinAliasMapping[F[_]] extends DoobieMapping[F] {

  object episode extends TableDef("episodes3") {
    val id = col("id", Meta[String])
    val name = col("name", Meta[String])
  }

  object image extends TableDef("images3") {
    val publicUrl = col("public_url", Meta[String])
    val id = col("id", Meta[String])
    val name = col("name", Meta[String])
  }

  val schema =
    schema"""
      type Query {
        episode(id: String!): Episode!
      }
      type Episode {
        id: String!
        name: String!
        images(filter: Filter): [Image!]!
      }
      type Image {
        id: String!
        publicUrl: String!
        inner: Inner!
      }
      type Inner {
        name: String!
      }
      input Filter {
        name: String
      }
    """

  val QueryType = schema.ref("Query")
  val EpisodeType = schema.ref("Episode")
  val ImageType = schema.ref("Image")
  val InnerType = schema.ref("Inner")

  val typeMappings =
    List(
      ObjectMapping(
        tpe = QueryType,
        fieldMappings =
          List(
            SqlRoot("episode")
          )
      ),
      ObjectMapping(
        tpe = EpisodeType,
        fieldMappings =
          List(
            SqlField("id", episode.id, key = true),
            SqlField("name", episode.name, key = true),
            SqlObject("images", Join(episode.id, image.id))
          )
      ),
      ObjectMapping(
        tpe = ImageType,
        fieldMappings =
          List(
            SqlField("id", image.id),
            SqlField("publicUrl", image.publicUrl, key = true),
            SqlObject("inner")
          )
      ),
      ObjectMapping(
        tpe = InnerType,
        fieldMappings =
          List(
            SqlField("name", image.name, key = true)
          )
      )
    )

  object FilterValue {
    def unapply(input: ObjectValue): Option[Predicate] = {
      input.fields match {
        case List(("name", StringValue(name))) =>
          Some(Eql(UniquePath(List("inner", "name")), Const(name)))
        case _ => None
      }
    }
  }

  def mkFilter(query: Query, filter: Value): Result[Query] = {
    filter match {
      case AbsentValue|NullValue => query.rightIor
      case FilterValue(pred) => Filter(pred, query).rightIor
      case _ => mkErrorResult(s"Expected filter value, found $filter")
    }
  }

  override val selectElaborator: SelectElaborator = new SelectElaborator(Map(
    QueryType -> {
      case Select("episode", List(Binding("id", StringValue(id))), child) =>
        Select(
          "episode",
          Nil,
          Unique(Filter(Eql(UniquePath[String](List("id")), Const(id)), child))
        ).rightIor
    },
    EpisodeType -> {
      case Select("images", List(Binding("filter", filter)), child) =>
        for {
          fc <- mkFilter(child, filter)
        } yield Select("images", Nil, fc)

      case other =>
        other.rightIor
    }
  ))
}

object FilterJoinAliasMapping extends LoggedDoobieMappingCompanion {
  def mkMapping[F[_]: Sync](transactor: Transactor[F], monitor: DoobieMonitor[F]): FilterJoinAliasMapping[F] =
    new DoobieMapping(transactor, monitor) with FilterJoinAliasMapping[F]
}

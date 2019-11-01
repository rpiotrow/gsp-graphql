// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.grackle
package doobie

import cats.effect.Bracket
import cats.implicits._
import _root_.doobie.{ Fragment, Transactor }
import _root_.doobie.implicits._
import io.chrisdavenport.log4cats.Logger
import io.circe.Json

import DoobieMapping._
import Query._
import QueryInterpreter.{ mkErrorResult, ProtoJson }

abstract class DoobieQueryInterpreter[F[_]](override implicit val F: Bracket[F, Throwable]) extends QueryInterpreter[F] {
  val mapping: DoobieMapping
  val xa: Transactor[F]
  val logger: Logger[F]

  def predicates(fieldName: String, args: List[Binding]): List[Fragment]

  def runRootValue(query: Query): F[Result[ProtoJson]] =
    query match {
      case Select(fieldName, args, child) =>
        val fieldTpe = schema.queryType.field(fieldName)
        val mapped = mapping.mapQuery(child, fieldTpe, predicates(fieldName, args))

        for {
          table <- logger.info(s"fetch(${mapped.fragment})") *> mapped.fetch.transact(xa)
          value <- runValue(child, fieldTpe, DoobieCursor(fieldTpe, table, mapped))
        } yield value

      case _ => mkErrorResult(s"Bad query").pure[F]
    }
}

case class DoobieCursor(val tpe: Type, val focus: Any, mapped: MappedQuery) extends Cursor {
  def asTable: Result[Table] = focus match {
    case table: List[_] => table.asInstanceOf[Table].rightIor
    case _ => mkErrorResult(s"Not a table")
  }

  def isLeaf: Boolean = tpe.isLeaf

  def asLeaf: Result[Json] =
    focus match {
      case s: String => Json.fromString(s).rightIor
      case i: Int => Json.fromInt(i).rightIor
      case d: Double => Json.fromDouble(d) match {
          case Some(j) => j.rightIor
          case None => mkErrorResult(s"Unrepresentable double %d")
        }
      case b: Boolean => Json.fromBoolean(b).rightIor
      case _ => mkErrorResult("Not a leaf")
    }

  def isList: Boolean =
    tpe match {
      case ListType(_) => true
      case _ => false
    }

  def asList: Result[List[Cursor]] =
    if (!tpe.isList) mkErrorResult(s"Not a list: $tpe")
    else {
      val itemTpe = tpe.item.dealias
      asTable.map(table => mapped.group(table, itemTpe).map(table => copy(tpe = itemTpe, focus = table)))
    }

  def isNullable: Boolean =
    tpe match {
      case NullableType(_) => true
      case _ => false
    }

  def asNullable: Result[Option[Cursor]] =
    (tpe, focus) match {
      case (NullableType(_), None) => None.rightIor
      case (NullableType(tpe), Some(v)) => Some(copy(tpe = tpe, focus = v)).rightIor
      case (NullableType(_), null) => None.rightIor
      case (NullableType(tpe), v) => Some(copy(tpe = tpe, focus = v)).rightIor
      case _ => mkErrorResult("Not nullable")
    }

  def hasField(fieldName: String): Boolean = {
    val fieldTpe = tpe.field(fieldName)
    if (fieldTpe.isLeaf)
      mapped.hasField(tpe, fieldName)
    else
      mapped.hasSubobject(fieldTpe.underlyingObject)
  }

  def field(fieldName: String, args: Map[String, Any]): Result[Cursor] = {
    val fieldTpe = tpe.field(fieldName)
    if (fieldTpe.isLeaf)
      asTable.map(table => copy(tpe = fieldTpe, focus = mapped.selectField(table.head, tpe, fieldName)))
    else
      copy(tpe = fieldTpe).rightIor
  }

  def hasAttribute(attributeName: String): Boolean =
    mapped.hasKey(tpe, attributeName)

  def attribute(attributeName: String): Result[Any] =
    asTable.map(table => mapped.selectKey(table.head, tpe, attributeName))
}

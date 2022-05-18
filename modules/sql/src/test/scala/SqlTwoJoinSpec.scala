// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package grackle.test

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import edu.gemini.grackle._
import edu.gemini.grackle.syntax._
import grackle.test.GraphQLResponseTests.assertWeaklyEqual
import io.circe.Json
import org.scalatest.funsuite.AnyFunSuite

trait SqlTwoJoinSpec extends AnyFunSuite {
  def mapping: QueryExecutor[IO, Json]

  test("two join query") {
    val query = """
      query {
        student {
          firstName
          lastName
          class {
            name
            enrollmentDate
          }
        }
      }
    """

    val expected = json"""
      {
        "data" : {
          "student" : [
            {
              "firstName": "Student",
              "lastName": "One",
              "class" : [
                {
                  "name" : "Class1",
                  "enrollmentDate" : "2021-10-01"
                }
              ]
            }
          ]
        }
      }
    """

    val res = mapping.compileAndRun(query).unsafeRunSync()
    println(res)

    assertWeaklyEqual(res, expected)
  }
}

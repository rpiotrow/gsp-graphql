// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package twojoin

import grackle.test.SqlTwoJoinSpec
import utils.DatabaseSuite

final class TwoJoinSpec extends DatabaseSuite with SqlTwoJoinSpec {
  lazy val mapping = TwoJoinData.fromTransactor(xa)

  test("validate") {
    val validationFailures = mapping.validator.validateMapping()

    validationFailures.foreach(p => println(p.toErrorMessage))
    assert(validationFailures.isEmpty)
  }
}

// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package filterjoinalias

import grackle.test.SqlFilterJoinAliasSpec
import utils.DatabaseSuite

final class FilterJoinAliasSpec extends DatabaseSuite with SqlFilterJoinAliasSpec {
  lazy val mapping = FilterJoinAliasMapping.fromTransactor(xa)
}

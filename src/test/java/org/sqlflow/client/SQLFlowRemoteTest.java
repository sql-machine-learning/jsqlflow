/*
 * Copyright 2019 The SQLFlow Authors. All rights reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sqlflow.client;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.sqlflow.client.utils.EnvironmentSpecificSQLFlowClient;

@RunWith(JUnit4.class)
public class SQLFlowRemoteTest {
  private SQLFlow client;

  @Before
  public void setUp() {
    client = EnvironmentSpecificSQLFlowClient.getClient(new MessageHandlerExample());
  }

  @Test
  public void runTest() {
    if (client == null) {
      System.out.println("skip remote test");
      return;
    }
    assert EnvironmentSpecificSQLFlowClient.hasGoodResponse(client, "SELECT 2");
  }
}

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

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import proto.Sqlflow.Session;

@RunWith(JUnit4.class)
public class SQLFlowRemoteTest {
  private SQLFlow client;

  @Before
  public void setUp() throws Exception {
    String serverAddr = System.getenv("SQLFLOW_SERVER");
    String submitter = System.getenv("SQLFLOW_SUBMITTER");
    String dataSource = System.getenv("SQLFLOW_DATA_SOURCE");
    String userId = System.getenv("USER_ID");
    if (StringUtils.isAnyBlank(serverAddr, submitter, dataSource, userId)) {
      return;
    }

    ManagedChannel chan = ManagedChannelBuilder.forTarget(serverAddr).usePlaintext().build();
    Session session =
        Session.newBuilder()
            .setUserId(userId)
            .setSubmitter(submitter)
            .setDbConnStr(dataSource)
            .build();
    client =
        SQLFlow.Builder.newInstance()
            .withSession(session)
            .withIntervalFetching(2000)
            .withMessageHandler(new MessageHandlerExample())
            .withChannel(chan)
            .build();
  }

  @Test
  public void testRun() {
    if (client == null) {
      System.out.println("skip remote test");
      return;
    }
    try {
      client.run("SELECT 1");
      client.release();
    } catch (Exception e) {
      assert false;
    }
  }
}

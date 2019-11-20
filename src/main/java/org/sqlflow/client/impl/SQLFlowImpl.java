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

package org.sqlflow.client.impl;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import java.util.concurrent.TimeUnit;
import org.sqlflow.client.SQLFlow;
import proto.SQLFlowGrpc;
import proto.Sqlflow.Job;
import proto.Sqlflow.JobStatus;
import proto.Sqlflow.Request;

public class SQLFlowImpl implements SQLFlow {
  private ManagedChannel channel;
  private SQLFlowGrpc.SQLFlowBlockingStub blockingStub;

  public void init(String serverUrl) {
    this.channel =
        ManagedChannelBuilder.forTarget(serverUrl).useTransportSecurity().usePlaintext().build();
    blockingStub = SQLFlowGrpc.newBlockingStub(channel);
  }

  public String submit(String sql) throws StatusRuntimeException {
    // TODO(weiguo) set Session
    Request req = Request.newBuilder().setSql(sql).build();
    try {
      Job job = blockingStub.submit(req);
      return job.getId();
    } catch (StatusRuntimeException e) {
      // TODO(weiguo) logger.error
      throw e;
    }
  }

  public JobStatus fetch(String jobId) throws StatusRuntimeException {
    Job req = Job.newBuilder().setId(jobId).build();
    try {
      return blockingStub.fetch(req);
    } catch (StatusRuntimeException e) {
      // TODO(weiguo) logger.error
      throw e;
    }
  }

  public void release() throws InterruptedException {
    try {
      channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      // TODO(weiguo) logger.error
      throw e;
    }
  }
}

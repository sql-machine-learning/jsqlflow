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

import java.nio.file.DirectoryIteratorException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import javafx.print.PrinterJob.JobStatus;
import org.apache.commons.lang3.StringUtils;
import org.sqlflow.client.SQLFlow;
import proto.SQLFlowGrpc;
import proto.Sqlflow.FetchRequest;
import proto.Sqlflow.FetchResponse;
import proto.Sqlflow.Job;
import proto.Sqlflow.Request;
import proto.Sqlflow.Response;
import proto.Sqlflow.Session;

public class SQLFlowImpl implements SQLFlow {
  private ManagedChannel channel;
  private SQLFlowGrpc.SQLFlowBlockingStub blockingStub;

  private SQLFlowImpl(Builder builder) {
    this.channel = builder.channel;
    blockingStub = SQLFlowGrpc.newBlockingStub(channel);
  }

  public String submit(Session session, String sql)
      throws IllegalArgumentException, StatusRuntimeException, NoSuchElementException {
    if (session == null || StringUtils.isAnyBlank(session.getDbConnStr(), session.getUserId())) {
      throw new IllegalArgumentException("data source and userId are not allowed to be empty");
    }
    if (StringUtils.isBlank(sql)) {
      throw new IllegalArgumentException("sql is empty");
    }

    Request req = Request.newBuilder().setSession(session).setSql(sql).build();
    try {
      Iterator<Response> responses = blockingStub.run(req);
      if (!responses.hasNext()) {
        throw new NoSuchElementException("bad response");
      }
      Response response = responses.next();
      return response.getJob().getId();
    } catch (StatusRuntimeException e) {
      // TODO(weiguo) logger.error
      throw e;
    }
  }

  public FetchResponse fetch(String jobId) throws StatusRuntimeException {
    Job job = Job.newBuilder().setId(jobId).build();
    FetchRequest req = FetchRequest.newBuilder().setJob(job).build();
    try {
      FetchResponse response = blockingStub.fetch(req);
      // response.getLogs() 内容+Log 暂时统一在 getLogs 中
      // 实现成循环方式更优
      FetchRequest updatedRequest = response.getUpdatedFetchSince();
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

  public static class Builder {
    private ManagedChannel channel;

    public static Builder newInstance() {
      return new Builder();
    }

    public Builder withChannel(ManagedChannel channel) {
      this.channel = channel;
      return this;
    }

    /**
     * Open a channel to the SQLFlow server. The serverUrl argument always ends with a port.
     *
     * @param serverUrl an address the SQLFlow server exposed.
     *     <p>Example: "localhost:50051"
     */
    public Builder forTarget(String serverUrl) {
      return withChannel(ManagedChannelBuilder.forTarget(serverUrl).usePlaintext().build());
    }

    public SQLFlow build() {
      return new SQLFlowImpl(this);
    }
  }
}

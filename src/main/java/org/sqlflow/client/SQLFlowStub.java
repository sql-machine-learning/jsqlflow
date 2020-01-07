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

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import com.google.protobuf.Any;
import com.google.protobuf.Parser;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import org.apache.commons.lang3.StringUtils;
import org.sqlflow.client.utils.HTMLDetector;
import proto.SQLFlowGrpc;
import proto.Sqlflow.FetchRequest;
import proto.Sqlflow.FetchResponse;
import proto.Sqlflow.FetchResponse.Logs;
import proto.Sqlflow.Job;
import proto.Sqlflow.Request;
import proto.Sqlflow.Response;
import proto.Sqlflow.Session;

public class SQLFlowStub {
  private Builder builder;

  private SQLFlowGrpc.SQLFlowBlockingStub blockingStub;
  // TODO(weiguo): It looks we need the futureStub to handle a large data set.
  // private SQLFlowGrpc.SQLFlowFutureStub futureStub;

  private SQLFlowStub(Builder builder) {
    this.builder = builder;
    blockingStub = SQLFlowGrpc.newBlockingStub(builder.channel);
  }

  public void run(String sql)
      throws IllegalArgumentException, StatusRuntimeException, NoSuchElementException,
          InterruptedException {
    if (StringUtils.isBlank(sql)) {
      throw new IllegalArgumentException("sql is empty");
    }

    Request req = Request.newBuilder().setSession(builder.session).setSql(sql).build();
    try {
      Iterator<Response> responses = blockingStub.run(req);
      handleSQLFlowResponses(responses);
    } catch (StatusRuntimeException e) {
      // TODO(weiguo) logger.error
      throw e;
    } finally {
      // TODO(weiguo) shall well release the channel here?
      builder.channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  private void handleSQLFlowResponses(Iterator<Response> responses) {
    if (responses == null || !responses.hasNext()) {
      throw new NoSuchElementException("bad response");
    }
    while (responses.hasNext()) {
      Response response = responses.next();
      if (response == null) {
        break;
      }
      if (response.hasHead()) {
        builder.handler.handleHeader(response.getHead().getColumnNamesList());
      } else if (response.hasRow()) {
        List<Any> rows = response.getRow().getDataList();
        // TODO(weiguo): parse `com.google.protobuf.Any` typed rows.
        builder.handler.handleRows(rows);
      } else if (response.hasMessage()) {
        String msg = response.getMessage().getMessage();
        if (HTMLDetector.validate(msg)) {
          builder.handler.handleHTML(msg);
        } else {
          builder.handler.handleText(msg);
        }
      } else if (response.hasEoe()) {
        builder.handler.handleEOE();
        // assert(!responses.hasNext())
      } else if (response.hasJob()) {
        trackingJobStatus(response.getJob().getId());
        // assert(!responses.hasNext())
      } else {
        break;
      }
    }
  }

  private void trackingJobStatus(String jobId) {
    Job job = Job.newBuilder().setId(jobId).build();
    FetchRequest req = FetchRequest.newBuilder().setJob(job).build();
    while (true) {
      FetchResponse response = blockingStub.fetch(req);
      Logs logs = response.getLogs();
      this.builder.handler.handleJob(logs.getContentList());
      if (response.getEof()) {
        this.builder.handler.handleEOE();
        break;
      }
      req = response.getUpdatedFetchSince();

      try {
        Thread.sleep(builder.intervalFetching);
      } catch (InterruptedException e) {
        break;
      }
    }
  }

  public static class Builder {
    private ManagedChannel channel;
    private MessageHandler handler;
    private Session session;
    private long intervalFetching = 2000L; // millis

    public static Builder newInstance() {
      return new Builder();
    }

    public Builder withChannel(ManagedChannel channel) {
      this.channel = channel;
      return this;
    }

    public Builder withMessageHandler(MessageHandler handler) {
      this.handler = handler;
      return this;
    }

    public Builder withIntervalFetching(long mills) {
      if (mills > 0) {
        this.intervalFetching = mills;
      }
      return this;
    }

    public Builder withSession(Session session) {
      if (session == null || StringUtils.isAnyBlank(session.getDbConnStr(), session.getUserId())) {
        throw new IllegalArgumentException("data source and userId are not allowed to be empty");
      }
      this.session = session;
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

    public SQLFlowStub build() {
      return new SQLFlowStub(this);
    }
  }
}

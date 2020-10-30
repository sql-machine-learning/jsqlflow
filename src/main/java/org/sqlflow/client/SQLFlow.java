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

import com.google.protobuf.Any;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.sqlflow.client.utils.HTMLDetector;
import proto.SQLFlowGrpc;
import proto.Sqlflow.FetchRequest;
import proto.Sqlflow.FetchResponse;
import proto.Sqlflow.Job;
import proto.Sqlflow.Request;
import proto.Sqlflow.Response;
import proto.Sqlflow.Session;

public class SQLFlow {
  private Builder builder;

  private SQLFlowGrpc.SQLFlowBlockingStub blockingStub;
  // TODO(weiguo): It looks we need the futureStub to handle a large data set.
  // private SQLFlowGrpc.SQLFlowFutureStub futureStub;

  private SQLFlow(Builder builder) {
    this.builder = builder;
    blockingStub = SQLFlowGrpc.newBlockingStub(builder.channel);
  }

  public void run(String sql)
      throws IllegalArgumentException, StatusRuntimeException, NoSuchElementException {
    if (StringUtils.isBlank(sql)) {
      throw new IllegalArgumentException("sql is empty");
    }

    Request req = Request.newBuilder().setSession(builder.session).setStmts(sql).build();
    try {
      Iterator<Response> responses = blockingStub.run(req);
      handleSQLFlowResponses(responses);
    } catch (StatusRuntimeException e) {
      // TODO(weiguo) logger.error
      throw e;
    }
  }

  public void release() throws InterruptedException {
    builder.channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
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
        List<Any> row = response.getRow().getDataList();
        builder.handler.handleRow(row);
      } else if (response.hasMessage()) {
        String msg = response.getMessage().getMessage();
        if (HTMLDetector.validate(msg)) {
          builder.handler.handleHTML(msg);
        } else {
          builder.handler.handleText(msg);
        }
      } else if (response.hasEoe()) {
        builder.handler.handleEOE();
      } else if (response.hasJob()) {
        trackingJobStatus(response.getJob().getId());
      } else {
        break;
      }
    }
  }

  private void trackingJobStatus(String jobId) {
    Job job = Job.newBuilder().setId(jobId).build();
    FetchRequest req = FetchRequest.newBuilder().setJob(job).build();
    while (true) {
      // move sleep to top, sleep a while for each message
      // especially for the first one in case the info is not
      // updated in k8s
      try {
        Thread.sleep(builder.intervalFetching);
      } catch (InterruptedException e) {
        break;
      }

      FetchResponse fr = blockingStub.fetch(req);
      List<Response> responses = fr.getResponses().getResponseList();
      responses.forEach(
          msg -> {
            if (msg.hasHead()) {
              builder.handler.handleHeader(msg.getHead().getColumnNamesList());
            } else if (msg.hasRow()) {
              List<Any> row = msg.getRow().getDataList();
              builder.handler.handleRow(row);
            } else if (msg.hasMessage()) {
              String content = msg.getMessage().getMessage();
              if (HTMLDetector.validate(content)) {
                builder.handler.handleHTML(content);
              } else {
                builder.handler.handleText(content);
              }
            } else if (msg.hasEoe()) {
              // A SQL program separated into several SQL statements.
              // A SQL program -> A SQLFlow job
              // FetchResponse.EOF: end of a SQLFlow Job
              // Response.EOE: end of a SQL statement.
              // User doesn't care about if a SQL statement finished, so let's skip the handleEOE
              // builder.handler.handleEOE();
            }
          });

      if (fr.getEof()) {
        this.builder.handler.handleEOE();
        break;
      }
      req = fr.getUpdatedFetchSince();
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
      if (session == null || StringUtils.isEmpty(session.getDbConnStr())) {
        throw new IllegalArgumentException("empty DB connection string is not allowed");
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

    public SQLFlow build() {
      return new SQLFlow(this);
    }
  }
}

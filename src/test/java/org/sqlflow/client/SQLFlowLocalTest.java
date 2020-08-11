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

import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import proto.SQLFlowGrpc;
import proto.Sqlflow.FetchRequest;
import proto.Sqlflow.FetchResponse;
import proto.Sqlflow.FetchResponse.Responses;
import proto.Sqlflow.Head;
import proto.Sqlflow.Job;
import proto.Sqlflow.Message;
import proto.Sqlflow.Request;
import proto.Sqlflow.Response;
import proto.Sqlflow.Session;

@RunWith(JUnit4.class)
public class SQLFlowLocalTest {
  private SQLFlow client;
  private static final String USER = "314159";
  /**
   * This rule manages automatic graceful shutdown for the registered servers and channels at the
   * end of test.
   */
  @Rule public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  private final SQLFlowGrpc.SQLFlowImplBase grpcService =
      mock(
          SQLFlowGrpc.SQLFlowImplBase.class,
          delegatesTo(
              new SQLFlowGrpc.SQLFlowImplBase() {
                public void run(Request req, StreamObserver<Response> rsp) {
                  Session session = req.getSession();
                  String userId = session.getUserId();
                  String submitter = session.getSubmitter();

                  Message msg =
                      Message.newBuilder().setMessage(submitter + ": hello " + userId).build();
                  rsp.onNext(Response.newBuilder().setMessage(msg).build());

                  Head header =
                      Head.newBuilder().addColumnNames("name").addColumnNames("salary").build();
                  rsp.onNext(Response.newBuilder().setHead(header).build());

                  Job job = Job.newBuilder().setId(mockJobId(userId, req.getSql())).build();
                  rsp.onNext(Response.newBuilder().setJob(job).build());
                  rsp.onCompleted();
                }

                public void fetch(FetchRequest req, StreamObserver<FetchResponse> rsp) {
                  String jobId = req.getJob().getId();
                  FetchRequest.Builder frb =
                      FetchRequest.newBuilder().setJob(Job.newBuilder().setId(jobId).build());
                  if (StringUtils.isEmpty(req.getStepId())) {
                    Responses rs =
                        Responses.newBuilder()
                            .addResponse(
                                Response.newBuilder()
                                    .setMessage(
                                        Message.newBuilder()
                                            .setMessage("fetchLogs for job=[" + jobId + "]")
                                            .build())
                                    .build())
                            .addResponse(
                                Response.newBuilder()
                                    .setMessage(Message.newBuilder().setMessage("1st line").build())
                                    .build())
                            .addResponse(
                                Response.newBuilder()
                                    .setMessage(Message.newBuilder().setMessage("2nd line").build())
                                    .build())
                            .addResponse(
                                Response.newBuilder()
                                    .setMessage(
                                        Message.newBuilder().setMessage("no more logs").build())
                                    .build())
                            .build();
                    rsp.onNext(
                        FetchResponse.newBuilder()
                            .setResponses(rs)
                            .setUpdatedFetchSince(frb.setStepId("1").build())
                            .build());
                  } else if (req.getStepId().equalsIgnoreCase("1")) {
                    rsp.onNext(
                        FetchResponse.newBuilder()
                            .setUpdatedFetchSince(frb.setStepId("2").build())
                            .setEof(false)
                            .build());
                  } else if (req.getStepId().equalsIgnoreCase("2")) {
                    Responses rs =
                        Responses.newBuilder()
                            .addResponse(
                                Response.newBuilder()
                                    .setMessage(Message.newBuilder().setMessage("bye").build())
                                    .build())
                            .build();
                    rsp.onNext(
                        FetchResponse.newBuilder()
                            .setResponses(rs)
                            .setUpdatedFetchSince(frb.setStepId("3").build())
                            .build());
                  } else if (req.getStepId().equalsIgnoreCase("3")) {
                    rsp.onNext(
                        FetchResponse.newBuilder()
                            .setEof(true)
                            .setUpdatedFetchSince(frb.setStepId("-1").build())
                            .build());
                  }
                  rsp.onCompleted();
                }
              }));

  @Before
  public void setUp() throws Exception {
    String serverName = InProcessServerBuilder.generateName();
    grpcCleanup.register(
        InProcessServerBuilder.forName(serverName)
            .directExecutor()
            .addService(grpcService)
            .build()
            .start());

    ManagedChannel channel =
        grpcCleanup.register(InProcessChannelBuilder.forName(serverName).directExecutor().build());

    Session session =
        Session.newBuilder()
            .setUserId(USER)
            .setSubmitter("pai")
            .setDbConnStr("mysql://root:root@127.0.0.1:3306/iris")
            .build();
    client =
        SQLFlow.Builder.newInstance()
            .withSession(session)
            .withIntervalFetching(500)
            .withMessageHandler(new MessageHandlerExample())
            .withChannel(channel)
            .build();
  }

  @Test
  public void testRun() {
    try {
      client.run("SELECT * TO TRAIN DNNClassifier WITH ... COLUMN ... INTO ..");
    } catch (Exception e) {
      assert false;
    } finally {
      try {
        client.release();
      } catch (InterruptedException e) {
        System.err.println("encounter an exception while releasing SQLFlow client");
      }
    }

    ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
    verify(grpcService).run(requestCaptor.capture(), ArgumentMatchers.any());
  }

  private String mockJobId(String userId, String sql) {
    return userId + "/" + sql;
  }
}

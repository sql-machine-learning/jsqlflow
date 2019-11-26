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

import static org.junit.Assert.assertEquals;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.sqlflow.client.impl.SQLFlowImpl;
import org.sqlflow.client.model.RequestHeader;
import proto.SQLFlowGrpc;
import proto.Sqlflow.Job;
import proto.Sqlflow.JobStatus;
import proto.Sqlflow.JobStatus.Code;
import proto.Sqlflow.Request;
import proto.Sqlflow.Session;

@RunWith(JUnit4.class)
public class SQLFlowTest {
  private SQLFlow client;
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
                public void submit(Request request, StreamObserver<Job> response) {
                  Session session = request.getSession();
                  String userId = session.getUserId();
                  response.onNext(
                      Job.newBuilder().setId(mockJobId(userId, request.getSql())).build());
                  response.onCompleted();
                }

                public void fetch(Job request, StreamObserver<JobStatus> response) {
                  String jobId = request.getId();
                  response.onNext(
                      JobStatus.newBuilder()
                          .setCodeValue(Code.PENDING_VALUE)
                          .setMessage(mockMessage(jobId))
                          .build());
                  response.onCompleted();
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
    client = new SQLFlowImpl(channel);
  }

  @Test
  public void testSubmit() {
    String userId = "314159";
    String sql = "SELECT * TO TRAIN DNNClassify WITH ... COLUMN ... INTO ..";

    ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
    RequestHeader header = new RequestHeader();
    header.setUserId(userId);
    header.setDataSource("mysql://root@root@127.0.0.1:3306/iris");
    String jobId = client.submit(header, sql);
    assertEquals(mockJobId(userId, sql), jobId);
    verify(grpcService)
        .submit(requestCaptor.capture(), ArgumentMatchers.<StreamObserver<Job>>any());
    assertEquals(sql, requestCaptor.getValue().getSql());
  }

  private String mockJobId(String userId, String sql) {
    return userId + "/" + sql;
  }

  private String mockMessage(String jobId) {
    return "Hello " + jobId;
  }

  @Test
  public void testFetch() {
    String jobId = "this is a job id";
    JobStatus jobStatus = client.fetch(jobId);

    assertEquals(Code.PENDING_VALUE, jobStatus.getCode().getNumber());
    assertEquals(mockMessage(jobId), jobStatus.getMessage());
  }

  @After
  public void tearDown() throws Exception {
    client.shutdown();
  }
}

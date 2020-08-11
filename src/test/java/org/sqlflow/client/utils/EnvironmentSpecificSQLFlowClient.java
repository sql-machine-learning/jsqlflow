package org.sqlflow.client.utils;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.apache.commons.lang3.StringUtils;
import org.sqlflow.client.MessageHandler;
import org.sqlflow.client.SQLFlow;
import proto.Sqlflow;

public class EnvironmentSpecificSQLFlowClient {
  public static SQLFlow getClient(MessageHandler messageHandler) {
    String serverAddr = System.getenv("SQLFLOW_SERVER");
    String submitter = System.getenv("SQLFLOW_SUBMITTER");
    String dataSource = System.getenv("SQLFLOW_DATA_SOURCE");
    String userId = System.getenv("USER_ID");
    if (StringUtils.isAnyBlank(serverAddr, submitter, dataSource, userId)) {
      return null;
    }

    ManagedChannel chan = ManagedChannelBuilder.forTarget(serverAddr).usePlaintext().build();
    Sqlflow.Session session =
        Sqlflow.Session.newBuilder()
            .setUserId(userId)
            .setSubmitter(submitter)
            .setDbConnStr(dataSource)
            .build();
    return SQLFlow.Builder.newInstance()
        .withSession(session)
        .withIntervalFetching(2000)
        .withMessageHandler(messageHandler)
        .withChannel(chan)
        .build();
  }
}

package org.sqlflow.client;

import org.apache.commons.lang3.StringUtils;
import org.sqlflow.client.utils.EnvironmentSpecificSQLFlowClient;

public class RunSQLFlowRemoteTest {
    public static void main(String[] args) {
        String sqlProgram = args.length == 0 ? null : args[0];
        if (StringUtils.isBlank(sqlProgram)) {
            System.out.println("skip the test due to the `sql` is null");
            return;
        }

        MessageHandler mh = new MessageHandlerExample();
        SQLFlow client = EnvironmentSpecificSQLFlowClient.getClient(mh);
        if (client == null) {
            System.out.println("skip the test due to the SQLFlow client is null");
            return;
        }
        assert EnvironmentSpecificSQLFlowClient.hasGoodResponse(client, sqlProgram);
    }
}

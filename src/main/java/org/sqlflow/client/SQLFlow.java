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

import io.grpc.StatusRuntimeException;
import proto.Sqlflow.JobStatus;
import proto.Sqlflow.Session;

public interface SQLFlow {
  /**
   * Submit a task to SQLFlow server. This method return immediately.
   *
   * @param session: specify dbConnStr(datasource), user Id ...
   *     mysql://root:root@tcp(localhost)/iris
   * @param sql: sql program.
   *     <p>Example: "SELECT * FROM iris.test; SELECT * FROM iris.iris TO TRAIN DNNClassifier
   *     COLUMN..." *
   * @return return a job id for tracking.
   * @throws IllegalArgumentException header or sql error
   * @throws StatusRuntimeException
   */
  String submit(Session session, String sql)
      throws IllegalArgumentException, StatusRuntimeException;

  /**
   * Fetch the job status by job id. The job id always returned by submit. By fetch(), we are able
   * to tracking the job status
   *
   * @param jobId specific the job we are going to track
   * @return see @code proto.JobStatus.Code
   * @throws StatusRuntimeException
   */
  JobStatus fetch(String jobId) throws StatusRuntimeException;

  /**
   * Close the opened channel to SQLFlow server. Waits for the channel to become terminated, giving
   * up if the timeout is reached.
   *
   * @throws InterruptedException thrown by awaitTermination
   */
  void release() throws InterruptedException;
}

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

import java.net.ConnectException;
import proto.Sqlflow.JobStatus;

public interface SQLFlow {
  /**
   * Open the connection(channel) to the SQLFlow server. The serverUrl argument always ends with a
   * port.
   *
   * @param serverUrl an address the SQLFlow server exposed.
   *     <p>Example: "localhost:50051"
   * @throws ConnectException when encountering the bad network.
   */
  void open(String serverUrl) throws ConnectException;

  /**
   * Submit a task to SQLFlow server. This method return immediately.
   *
   * @param sql: sql program. *
   *     <p>Example: "SELECT * FROM iris.test; SELECT * FROM iris.iris TO TRAIN DNNClassifier
   *     COLUMN..." *
   * @return return a job id for tracking.
   * @throws Exception TODO(weiguo): more precise
   */
  String submit(String sql) throws Exception;

  /**
   * Fetch the job status by job id. The job id always returned by submit. By fetch(), we are able
   * to tracking the job status
   *
   * @param jobId specific the job we are going to track
   * @return see @code proto.JobStatus.Code
   * @throws Exception TODO(weiguo): more precise
   */
  JobStatus fetch(String jobId) throws Exception;

  /**
   * Close the opened connection(channel) to SQLFlow server
   *
   * @throws Exception TODO(weiguo): more precise
   */
  void close() throws Exception;
}

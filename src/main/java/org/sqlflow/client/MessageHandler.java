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

import java.util.List;

/** Interface for application callback objects to receive logs and messages from SQLFlow server. */
public interface MessageHandler {
  /** Handle the html format response */
  void handleHTML(String html);

  /** Handle the log */
  void handleText(String text);

  /** EndOfExecution */
  void handleEOE();

  /** Table header. */
  void handleHeader(List<String> columnNames);

  /**
   * Results
   *
   * <p>TODO(weiguo): shouldn't expose `Any`
   */
  void handleRows(List<com.google.protobuf.Any> rows);
}

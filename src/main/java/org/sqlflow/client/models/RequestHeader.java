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

package org.sqlflow.client.models;

import lombok.Data;

@Data
public class RequestHeader {
  /**
   * database source, for:
   *
   * <p>maxcomputer
   * maxcompute://{accesskey_id}:{accesskey_secret}@{endpoint}?curr_project={curr_project}&scheme={scheme}
   *
   * <p>mysql
   * mysql://{username}:{password}@tcp({address})/{dbname}[?param1=value1&...&paramN=valueN]
   *
   * <p>hive
   * hive://user:password@ip:port/dbname[?auth=<auth_mechanism>&session.<cfg_key1>=<cfg_value1>...&session<cfg_keyN>=valueN]
   */
  private String dataSource;

  /** user who submits the SQL task. */
  private String userId;

  /* for alps */
  private boolean exitOnSubmit;

  /* hive */
  private String hiveLocation;
  private String hdfsNameNode;
  private String hdfsUser;
  private String hdfsPassword;
}

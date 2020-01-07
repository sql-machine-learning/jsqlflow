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

package org.sqlflow.client.utils;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * Test if a string contains HTML tag.
 *
 * <p>Thanks https://ideone.com/HakdHo
 */
public class HTMLDetector {
  private static final String TAG_START =
      "<\\w+((\\s+\\w+(\\s*=\\s*(?:\".*?\"|'.*?'|[^'\">\\s]+))?)+\\s*|\\s*)>";
  private static final String TAG_END = "</\\w+>";
  private static final String TAG_SELF_CLOSING =
      "<\\w+((\\s+\\w+(\\s*=\\s*(?:\".*?\"|'.*?'|[^'\">\\s]+))?)+\\s*|\\s*)/>";
  private static final String HTML_ENTITY = "&[a-zA-Z][a-zA-Z0-9]+;";
  private static final Pattern HTML_PATTERN =
      Pattern.compile(
          "(" + TAG_START + ".*" + TAG_END + ")|(" + TAG_SELF_CLOSING + ")|(" + HTML_ENTITY + ")",
          Pattern.DOTALL);

  public static boolean validate(String str) {
    return !StringUtils.isEmpty(str) && HTML_PATTERN.matcher(str).find();
  }
}

// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.wip.tools.protocolgenerator;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;


/**
 * Contains commonly used code snippets related to streams from java.io.
 */
class StreamUtil {
  static String readStringFromStream(InputStream stream, Charset charset) throws IOException {
    StringBuilder stringBuilder = new StringBuilder();

    Reader reader = new InputStreamReader(stream, charset);

    char[] buffer = new char[1024];
    while (true) {
      int res = reader.read(buffer);
      if (res == -1) {
        break;
      }
      stringBuilder.append(buffer, 0, res);
    }
    reader.close();
    stream.close();

    return stringBuilder.toString();
  }

  static final Charset UTF8_CHARSET = Charset.forName("UTF-8");
}

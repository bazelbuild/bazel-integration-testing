// Copyright 2016 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package build.bazel.tests.integration;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.Collections;


/**
 * A wrapper output stream to return the output as a list of string.
 */
final class LineListOutputStream extends OutputStream {

  private boolean closed = false;
  private List<String> lines = new LinkedList<>();

  private ByteArrayOutputStream stream = new ByteArrayOutputStream();

  /** Create a LineListOutputStream. */
  public LineListOutputStream() {
    super();
  }

  @Override
  public void write(int b) throws IOException {
    assert closed == false : "Attempted to write on a closed stream";
    byte b0 = (byte) b;
    if (b0 == '\n') {
      lines.add(stream.toString(StandardCharsets.UTF_8.name()));
      stream.reset();
    } else {
      stream.write(b);
    }
  }

  @Override
  public void close() throws IOException {
    assert closed == false;
    super.close();
    if (stream.size() > 0) {
      lines.add(stream.toString(StandardCharsets.UTF_8.name()));
      stream.reset();
    }
    closed = true;
  }

  /**
   * Returns the list of selected lines.
   */

  List<String> getLines() {
    return Collections.unmodifiableList(lines);
  }
}

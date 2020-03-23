// Copyright 2018 The Bazel Authors. All rights reserved.
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

package testdata;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Executable {
  public static void main(String[] args) throws IOException {
    assertArrayEquals(new String[] {"hello", "world"}, args);
    Path dataFile =
        Paths.get(
            System.getenv("JAVA_RUNFILES"), "build_bazel_integration_testing/testdata/data.txt");
    assertEquals("Foo bar", String.join("\n", Files.readAllLines(dataFile)));
  }
}

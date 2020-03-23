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

package build.bazel.tests.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** This class wraps the result of testing one single Bazel target. */
public class TestResult {

  private final String label;
  private final List<Path> files;

  public TestResult(String label, List<Path> files) {
    this.label = label;
    this.files = files;
  }

  /** Returns the label of the test target. */
  public String label() {
    return label;
  }

  /** Returns a list of output files generated during testing. */
  public List<Path> files() {
    return files;
  }

  /**
   * Returns an optional of the path to an output text file with a given basename, or an empty
   * optional if no file with this basename could be found. If there are multiple files with the
   * given basename, an error is thrown.
   */
  public Optional<Path> file(Path basename) {
    List<Path> filtered =
        files.stream().filter(it -> it.getFileName().equals(basename)).collect(Collectors.toList());
    if (filtered.isEmpty()) {
      return Optional.empty();
    } else if (filtered.size() > 1) {
      throw new RuntimeException(
          "expected one output file with basename '"
              + basename
              + "', got: "
              + filtered
                  .stream()
                  .map(it -> it.toAbsolutePath().toString())
                  .collect(Collectors.joining(", ")));
    }
    return Optional.of(filtered.get(0));
  }

  /**
   * Returns the lines of an output text file with a given basename, or an empty list if no file
   * with this basename could be found. If there are multiple files with the given basename, an
   * error is thrown.
   */
  public List<String> lines(Path basename) throws IOException {
    Optional<Path> file = file(basename);
    if (!file.isPresent()) {
      return Collections.emptyList();
    }
    return Files.readAllLines(file.get());
  }

  /**
   * Returns the content of an output text file with a given basename.
   *
   * @see #lines(Path)
   */
  public String content(Path basename) throws IOException {
    return String.join("\n", lines(basename));
  }

  /** Basename of the output file containing test logs. */
  public static final Path TEST_LOG = Paths.get("test.log");

  /**
   * Basename of the output file containing an ant-compatible test suite description. Parse this
   * file if you want, e.g., to read the stderr and stdout separately and without the wrapping that
   * is found in {@link #TEST_LOG}.
   */
  public static final Path TEST_XML = Paths.get("test.xml");

  /** Basename of the output file containing LCOV-formatted coverage data. */
  public static final Path COVERAGE_DAT = Paths.get("coverage.dat");

  /** Basename of the output file containing LCOV-formatted baseline coverage data. */
  public static final Path BASELINE_COVERAGE_DAT = Paths.get("baseline_coverate.dat");
}

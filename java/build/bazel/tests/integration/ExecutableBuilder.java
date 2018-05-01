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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ExecutableBuilder {
  public static final List<String> DEFAULT_VISIBILITY = Arrays.asList("//visibility:public");

  private final WorkspaceDriver driver;
  private final String packageName;
  private final String targetName;
  private List<String> visibility;
  private Path executable;
  private List<String> runfiles;

  public ExecutableBuilder(WorkspaceDriver driver, String packageName, String targetName) {
    this.driver = driver;
    this.packageName = packageName;
    this.targetName = targetName;
    this.visibility = DEFAULT_VISIBILITY;
  }

  public ExecutableBuilder manifest(String manifest) {
    List<String> manifestLines;
    try {
      manifestLines = Files.readAllLines(WorkspaceDriver.runfile(manifest));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    if (manifestLines.size() < 2) {
      throw new RuntimeException("unexpected manifest");
    }
    executable = WorkspaceDriver.runfile(manifestLines.get(0));
    if (!Files.exists(executable)) {
      throw new RuntimeException("executable not found: '" + executable.toAbsolutePath() + "'");
    }
    runfiles = manifestLines.subList(1, manifestLines.size());
    return this;
  }

  public ExecutableBuilder visibility(String... visibility) {
    return visibility(Arrays.asList(visibility));
  }

  public ExecutableBuilder visibility(List<String> visibility) {
    this.visibility = visibility;
    return this;
  }

  public void scratch() throws IOException {
    driver.appendToScratchFile(
        packageName + "/BUILD",
        "sh_binary(",
        "    name = \"" + targetName + "\",",
        "    srcs = [\"" + targetName + ".sh\"],",
        visibility
            .stream()
            .map(it -> "\"" + it + "\"")
            .collect(Collectors.joining(", ", "    visibility = [", "],")),
        ")");
    StringBuilder symlinks = new StringBuilder();
    for (String runfile : runfiles) {
      symlinks
          .append("mkdir -p \"$(dirname ")
          .append(runfile)
          .append(")\"\n")
          .append("ln -s \"")
          .append(WorkspaceDriver.runfile(runfile).toAbsolutePath())
          .append("\" \"")
          .append(runfile)
          .append("\"\n");
    }
    driver.scratchExecutableFile(
        packageName + "/" + targetName + ".sh",
        "#!/bin/bash",
        "set -eou pipefail",
        "TMPDIR=$(mktemp -d)",
        "mkdir -p \"$TMPDIR/" + targetName + ".runfiles\"",
        "pushd \"$TMPDIR/" + targetName + ".runfiles\"",
        symlinks.toString(),
        "popd",
        "ln -s \"" + executable.toAbsolutePath() + "\" \"$TMPDIR/" + targetName + "\"",
        "set -e",
        "\"$TMPDIR/" + targetName + "\" \"$@\"",
        "EXIT_CODE=$?",
        "if (( $? )); then",
        "  rm -rf \"$TMPDIR\"",
        "  exit $EXIT_CODE",
        "fi",
        "rm -rf \"$TMPDIR\"");
  }
}

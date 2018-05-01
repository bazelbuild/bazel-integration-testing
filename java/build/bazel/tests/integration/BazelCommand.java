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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** This class holds the result of a Bazel invocation. */
public class BazelCommand {

  private final Command delegate;
  private final List<String> args;
  private final int exitCode;
  private final WorkspaceDriver driver;

  private BazelCommand(Command delegate, List<String> args, int exitCode, WorkspaceDriver driver) {
    this.delegate = delegate;
    this.args = args;
    this.exitCode = exitCode;
    this.driver = driver;
  }

  /** Returns the exit code. */
  public int exitCode() {
    return exitCode;
  }

  /** Returns the list of lines of the standard error stream. */
  public List<String> errorLines() {
    return delegate.getErrorLines();
  }

  /** Returns the list of lines of the standard output stream. */
  public List<String> outputLines() {
    return delegate.getOutputLines();
  }

  /**
   * Returns a description/summary of the command arguments, standard error and output stream. This
   * method can be used for troubleshooting and error reporting.
   */
  public String toString() {
    String description =
        "BAZEL COMMAND: "
            + args
            + "\n"
            + "EXIT CODE: "
            + exitCode
            + "\nSTDOUT:\n    "
            + String.join("\n    ", outputLines())
            + "\nSTDERR:\n    "
            + String.join("\n    ", errorLines())
            + "\nWORKSPACE CONTENTS:\n    "
            + driver
                .workspaceDirectoryContents()
                .stream()
                .map(Path::toString)
                .collect(Collectors.joining("\n    "))
            + "\n";

    return description;
  }

  static class Builder {
    private final WorkspaceDriver driver;
    private final Path outputUserRoot;
    private final RepositoryCache repositoryCache;
    private final List<String> args;
    private Path bazelrcFile = null;
    private Map<String, String> environment = new HashMap<>();
    private Path workingDirectory = Paths.get("");

    Builder(
        WorkspaceDriver driver,
        Path outputUserRoot,
        RepositoryCache repositoryCache,
        List<String> args) {
      this.driver = driver;
      this.outputUserRoot = outputUserRoot;
      this.repositoryCache = repositoryCache;
      this.args = Collections.unmodifiableList(args);
    }

    /** sets the working directory relative to workspace path */
    public Builder inWorkingDirectory(Path dirRelativeToWorkspaceRoot) {
      this.workingDirectory = dirRelativeToWorkspaceRoot;
      return this;
    }

    /** sets the bazelrc file path */
    public Builder withBazelrcFile(Path bazelrcFile) {
      this.bazelrcFile = bazelrcFile;
      return this;
    }

    /** adds environment variable to execution runtime */
    public Builder withEnvironmentVariable(String key, String value) {
      this.environment.put(key, value);
      return this;
    }

    private Command build() throws IOException {
      String bazelRcPath =
          bazelrcFile == null
              ? "/dev/null"
              : driver.currentWorkspace().resolve(bazelrcFile).toString();

      List<String> command =
          new ArrayList<>(
              Arrays.asList(
                  driver.bazelBinPath().toString(),
                  "--output_user_root=" + outputUserRoot,
                  "--nomaster_bazelrc",
                  "--max_idle_secs=10",
                  "--bazelrc=" + bazelRcPath));

      // This would split the args "run //target -- hello world" into
      // "run //target" and "-- hello world" ("hello world" being passed to the executable
      // to run).
      int terminator = args.indexOf("--");
      if (terminator == -1) {
        command.addAll(args);
        command.addAll(repositoryCache.bazelOptions());
      } else {
        command.addAll(args.subList(0, terminator));
        command.addAll(repositoryCache.bazelOptions());
        command.addAll(args.subList(terminator, args.size()));
      }

      Path relativeToWorkspaceFullPath = driver.currentWorkspace().resolve(workingDirectory);

      return Command.builder()
          .setDirectory(relativeToWorkspaceFullPath)
          .addArguments(command)
          .withEnvironment(environment)
          .build();
    }

    /** Runs the command and returns an object to inspect the invocation result. */
    public BazelCommand run() throws IOException, InterruptedException {
      Command cmd = build();
      return new BazelCommand(cmd, args, cmd.run(), driver);
    }

    /**
     * Runs the command, throws an exception if the command does not succeed, and returns an object
     * to inspect the invocation result.
     */
    public BazelCommand mustRunSuccessfully() throws IOException, InterruptedException {
      BazelCommand cmd = run();
      if (cmd.exitCode() != 0) {
        throw new RuntimeException(cmd + "==> non-zero exit code");
      }
      return cmd;
    }
  }
}

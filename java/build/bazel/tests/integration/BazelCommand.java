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

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos;
import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos.BuildEvent;
import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos.BuildEventId.IdCase;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

/** This class holds the result of a Bazel invocation. */
public class BazelCommand {

  private final Command delegate;
  private final List<String> args;
  private final int exitCode;
  private final WorkspaceDriver driver;
  private final Path buildEventFile;
  // Lazy evaluation:
  private List<BuildEvent> buildEvents = null;
  private Map<String, Path> artifacts = null;

  private BazelCommand(
      Command delegate,
      List<String> args,
      int exitCode,
      Path buildEventFile,
      WorkspaceDriver driver) {
    this.delegate = delegate;
    this.args = args;
    this.exitCode = exitCode;
    this.buildEventFile = buildEventFile;
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
    StringBuilder description = new StringBuilder();

    description
        .append("BAZEL COMMAND: ")
        .append(args)
        .append("\n")
        .append("EXIT CODE: ")
        .append(exitCode)
        .append("\nSTDOUT:\n    ")
        .append(String.join("\n    ", outputLines()))
        .append("\nSTDERR:\n    ")
        .append(String.join("\n    ", errorLines()))
        .append("\nWORKSPACE CONTENTS:\n    ")
        .append(
            driver
                .workspaceDirectoryContents()
                .stream()
                .map(Path::toString)
                .collect(Collectors.joining("\n    ", "\nWORKSPACE CONTENTS:\n    ", "\n")));

    Map<String, Path> artifacts = artifacts();
    description.append(
        artifacts
            .entrySet()
            .stream()
            .map(it -> it.getKey() + "(" + it.getValue() + ")")
            .collect(Collectors.joining("\n    ", "ARTIFACTS:\n    ", "\n")));

    Map<String, TestResult> testResults = testResults();
    if (!testResults.isEmpty()) {
      description.append("\nTESTS:");
      for (TestResult testResult : testResults.values()) {
        String logs;
        try {
          logs = testResult.content(TestResult.TEST_LOG);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        description
            .append("\n  ")
            .append(testResult.label())
            .append("\n    ")
            .append(logs == null ? "  <logs: unavailable>" : logs);
      }
    }

    return description.toString();
  }

  /** Returns a map of artifact names to paths. */
  public Map<String, Path> artifacts() {
    if (artifacts == null) {
      artifacts =
          buildEvents()
              .stream()
              .flatMap(ev -> ev.getNamedSetOfFiles().getFilesList().stream())
              .collect(
                  Collectors.toMap(
                      BuildEventStreamProtos.File::getName, BazelCommand::bepFileToPath));
    }
    return artifacts;
  }

  /**
   * Returns an optional of the path to an artifact with a given name, or an empty optional if the
   * artifact could not be found.
   */
  public Optional<Path> artifact(String name) {
    return Optional.ofNullable(artifacts().get(name));
  }

  /** Returns a map of target labels to test results. */
  public Map<String, TestResult> testResults() {
    return buildEvents()
        .stream()
        .filter(ev -> ev.getId().getIdCase() == IdCase.TEST_RESULT)
        .map(
            ev ->
                new TestResult(
                    ev.getId().getTestResult().getLabel(),
                    ev.getTestResult()
                        .getTestActionOutputList()
                        .stream()
                        .map(BazelCommand::bepFileToPath)
                        .collect(Collectors.toList())))
        .collect(Collectors.toMap(TestResult::label, x -> x));
  }

  /** Ensures that there is one and only one test result and returns it. */
  public TestResult testResult() {
    Map<String, TestResult> testResults = testResults();
    Iterator<Entry<String, TestResult>> it = testResults.entrySet().iterator();
    if (!it.hasNext()) {
      throw new RuntimeException("no test result was found");
    }
    TestResult testResult = it.next().getValue();
    if (it.hasNext()) {
      throw new RuntimeException(
          "multiple test results were found: targets: " + String.join(", ", testResults.keySet()));
    }
    return testResult;
  }

  /** Returns the raw build event stream, for custom processing. */
  public List<BuildEvent> buildEvents() {
    if (buildEvents == null) {
      buildEvents = new ArrayList<>();
      try (InputStream inputStream = new FileInputStream(buildEventFile.toFile())) {
        BuildEvent event;
        while ((event = BuildEvent.parseDelimitedFrom(inputStream)) != null) {
          buildEvents.add(event);
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return buildEvents;
  }

  /** Converts a Build Event Protocol file to a {@link java.nio.file.Path}. */
  private static Path bepFileToPath(BuildEventStreamProtos.File bepFile) {
    try {
      return Paths.get(new URI(bepFile.getUri()));
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  public static class Builder {
    private final WorkspaceDriver driver;
    private final Path outputUserRoot;
    private final RepositoryCache repositoryCache;
    private final List<String> args;
    private Path buildEventFile;
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

      buildEventFile = Files.createTempFile(outputUserRoot, "bep", ".bp");

      // This would split the args "run //target -- hello world" into
      // "run //target" and "-- hello world" ("hello world" being passed to the executable
      // to run).
      int terminator = args.indexOf("--");
      if (terminator == -1) {
        command.addAll(args);
        command.addAll(repositoryCache.bazelOptions());
        // Notice that this option entails a 1s penalty on the execution time.
        command.add("--build_event_binary_file=" + buildEventFile.toAbsolutePath());
      } else {
        command.addAll(args.subList(0, terminator));
        command.addAll(repositoryCache.bazelOptions());
        command.add("--build_event_binary_file=" + buildEventFile.toAbsolutePath());
        command.addAll(args.subList(terminator, args.size()));
      }

      Path relativeToWorkspaceFullPath = driver.currentWorkspace().resolve(workingDirectory);

      return Command.builder()
          .setDirectory(relativeToWorkspaceFullPath)
          .addArguments(command)
          .withEnvironment(environment)
          .build();
    }

    /** Runs the command, prints its output to console and
     * returns an object to inspect the invocation result. */
    public BazelCommand runVerbose() throws IOException, InterruptedException {
      BazelCommand command = run();
      System.out.println(command.toString());
      return command;
    }

    /** Runs the command and returns an object to inspect the invocation result. */
    public BazelCommand run() throws IOException, InterruptedException {
      Command cmd = build();
      return new BazelCommand(cmd, args, cmd.run(), buildEventFile, driver);
    }

    /**
     * Runs the command, throws an exception if the command returns a different exit code than requested,
     * and returns an object to inspect the invocation result.
     */
    public BazelCommand mustRunAndReturnExitCode(int exitCode) throws IOException, InterruptedException {
      BazelCommand cmd = run();
      if (cmd.exitCode() != exitCode) {
        throw new RuntimeException(cmd + "==> exit code != " + exitCode);
      }
      return cmd;
    }

    /**
     * Runs the command, throws an exception if the command does not succeed, and returns an object
     * to inspect the invocation result.
     */
    public BazelCommand mustRunSuccessfully() throws IOException, InterruptedException {
      return mustRunAndReturnExitCode(0);
    }
  }
}

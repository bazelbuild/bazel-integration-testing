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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A utility class to spawn a command and get its output.
 *
 * <p>This class can only be initialized using a builder created with the {@link #builder()} method.
 */
public final class Command {

  private final Path directory;
  private final List<String> args;
  private final List<String> stderr = Collections.synchronizedList(new LinkedList<>());
  private final List<String> stdout = Collections.synchronizedList(new LinkedList<>());
  private final Map<String, String> environment;
  private boolean executed = false;

  private Command(Path directory, List<String> args, Map<String, String> environment) {
    this.directory = directory;
    this.args = args;
    this.environment = environment;
  }

  /**
   * Executes the command represented by this instance, and return the exit code of the command.
   * This method should not be called twice on the same object.
   */
  public int run() throws IOException, InterruptedException {
    assert !executed;
    executed = true;
    ProcessBuilder builder = new ProcessBuilder(args);
    builder.directory(directory.toFile());
    builder.environment().putAll(environment);
    builder.redirectOutput(ProcessBuilder.Redirect.PIPE);
    builder.redirectError(ProcessBuilder.Redirect.PIPE);
    Process process = builder.start();
    Thread err = streamToLinesThread(process.getErrorStream(), stderr);
    // seriously? That's stdout, why is it called getInputStream???
    Thread out = streamToLinesThread(process.getInputStream(), stdout);
    int exitCode = process.waitFor();
    if (err != null) {
      err.join();
    }
    if (out != null) {
      out.join();
    }

    return exitCode;
  }

  private static Thread streamToLinesThread(
      final InputStream inputStream, final List<String> lines) {
    Thread thread =
        new Thread(
            () -> {
              new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                  .lines()
                  .forEach(lines::add);
            });
    thread.start();
    return thread;
  }

  /** Returns the list of lines of the standard error stream. */
  public List<String> getErrorLines() {
    synchronized (stderr) {
      return copyToUnmodifiableList(stderr);
    }
  }

  /** Returns the list of lines of the standard output stream. */
  public List<String> getOutputLines() {
    synchronized (stdout) {
      return copyToUnmodifiableList(stdout);
    }
  }

  private static <T> List<T> copyToUnmodifiableList(final List<T> source) {
    return Collections.unmodifiableList(new LinkedList<>(source));
  }

  /** A builder class to generate a Command object. */
  static class Builder {

    private Path directory;
    private List<String> args = new ArrayList<>();
    private Map<String, String> environment = new HashMap<>();

    private Builder() {
      // Default to the current working directory
      this.directory = Paths.get(System.getProperty("user.dir"));
    }

    /**
     * Set the working directory for the program, it is set to the current working directory of the
     * current java process by default.
     */
    public Builder setDirectory(Path directory) {
      this.directory = directory;
      return this;
    }

    /**
     * Add arguments to the command line. The first argument to be added to the builder is the
     * program name.
     */
    public Builder addArguments(String... args) {
      this.args.addAll(Arrays.asList(args));
      return this;
    }

    /**
     * Add a list of arguments to the command line. The first argument to be added to the builder is
     * the program name.
     */
    public Builder addArguments(Iterable<String> args) {
      for (String arg : args) {
        this.args.add(arg);
      }
      return this;
    }

    /** Sets environment variable in the runtime */
    public Builder withEnvironment(Map<String, String> environment) {
      this.environment = Collections.unmodifiableMap(environment);
      return this;
    }

    /** Build a Command object. */
    public Command build() {
      Objects.requireNonNull(directory);
      List<String> args = Collections.unmodifiableList(this.args);
      Map<String, String> env = Collections.unmodifiableMap(environment);
      return new Command(directory, args, env);
    }
  }

  /** Returns a {@link Builder} object to use to create a {@link Command} object. */
  static Builder builder() {
    return new Builder();
  }
}

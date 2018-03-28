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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Function;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;


/**
 * A utility class to spawn a command and get its output.
 *
 * <p>
 * This class can only be initialized using a builder created with the {@link #builder()} method.
 */
final public class Command {

  private final File directory;
  private final List<String> args;
  private final LineListOutputStream stderr = new LineListOutputStream();
  private final LineListOutputStream stdout = new LineListOutputStream();
  private boolean executed = false;

  private Command(File directory, List<String> args) throws IOException {
    this.directory = directory;
    this.args = args;
  }

  /**
   * Executes the command represented by this instance, and return the exit code of the command.
   * This method should not be called twice on the same object.
   */
  public int run() throws IOException, InterruptedException {
    assert !executed;
    executed = true;
    ProcessBuilder builder = new ProcessBuilder(args);
    builder.directory(directory);
    builder.redirectOutput(ProcessBuilder.Redirect.PIPE);
    builder.redirectError(ProcessBuilder.Redirect.PIPE);
    Process process = builder.start();
    Thread err = copyStream(process.getErrorStream(), stderr);
    // seriously? That's stdout, why is it called getInputStream???
    Thread out = copyStream(process.getInputStream(), stdout);
    int r = process.waitFor();
    if (err != null) {
      err.join();
    }
    if (out != null) {
      out.join();
    }
    synchronized (stderr) {
      stderr.close();
    }
    synchronized (stdout) {
      stdout.close();
    }
    return r;
  }

  private static class CopyStreamRunnable implements Runnable {
    private final InputStream inputStream;
    private final OutputStream outputStream;

    CopyStreamRunnable(InputStream inputStream, OutputStream outputStream) {
      this.inputStream = inputStream;
      this.outputStream = outputStream;
    }

    @Override
    public void run() {
      byte[] buffer = new byte[4096];
      int read;
      try {
        while ((read = inputStream.read(buffer)) > 0) {
          synchronized (outputStream) {
            outputStream.write(buffer, 0, read);
          }
        }
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
    }
  }

  // Launch a thread to copy all data from inputStream to outputStream
  private static Thread copyStream(InputStream inputStream, OutputStream outputStream) {
    if (outputStream != null) {
      Thread t = new Thread(new CopyStreamRunnable(inputStream, outputStream), "CopyStream");
      t.start();
      return t;
    }
    return null;
  }

  /**
   * Returns the list of lines of the standard error stream.
   */
  public List<String> getErrorLines() {
    return stderr.getLines();
  }

  /**
   * Returns the list of lines of the standard output stream.
   */
  public List<String> getOutputLines() {
    return stdout.getLines();
  }

  /**
   * A builder class to generate a Command object.
   */
  static class Builder {

    private File directory;
    private List<String> args = new ArrayList<String>();

    private Builder() {
      // Default to the current working directory
      this.directory = new File(System.getProperty("user.dir"));
    }

    /**
     * Set the working directory for the program, it is set to the current working directory of the
     * current java process by default.
     */
    public Builder setDirectory(File directory) {
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

    /**
     * Build a Command object.
     */
    public Command build() throws IOException {
      Objects.requireNonNull(directory);
      List<String> args = Collections.unmodifiableList(this.args);
      return new Command(directory, args);
    }
  }

  /**
   * Returns a {@link Builder} object to use to create a {@link Command} object.
   */
  static Builder builder() {
    return new Builder();
  }
}

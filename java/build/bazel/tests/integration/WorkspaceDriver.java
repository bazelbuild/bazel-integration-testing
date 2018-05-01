// Copyright 2017 The Bazel Authors. All rights reserved.
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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WorkspaceDriver {

  private static Path tmp;
  private static Map<String, Path> bazelVersions;
  private static Path runfileDirectory = Paths.get(System.getenv("TEST_SRCDIR"));
  // TODO: Don't have them as writable package-private when we have a better way
  // to share configuration across the WorkspaceDriver instances.
  @Deprecated static Properties properties;

  private Path currentBazel = null;

  /** The current workspace. */
  private Path workspace = null;

  private static RepositoryCache repositoryCache;

  /** Returns the current workspace path */
  public Path currentWorkspace() {
    return workspace;
  }

  public static void setUpClass() throws IOException {
    loadProperties();
    setupTmp();
    bazelVersions = new HashMap<>();
    setupRepositoryCache();
  }

  private static void setupRepositoryCache() throws IOException {
    String externalDeps = properties.getProperty("bazel.external.deps");
    repositoryCache = new RepositoryCache(tmp.resolve("cache"));
    if (externalDeps != null && !externalDeps.isEmpty()) {
      for (String dep : externalDeps.split(",")) {
        repositoryCache.put(Paths.get(dep));
      }
    }
    repositoryCache.freeze();
  }

  private static void setupTmp() throws IOException {
    String environmentTempDirectory = System.getenv("TEST_TMPDIR");
    if (environmentTempDirectory == null) {
      tmp = Files.createTempDirectory("e4b-tests");
      tmp.toFile().deleteOnExit();
    } else {
      tmp = Paths.get(environmentTempDirectory);
    }
  }

  private static void loadProperties() throws IOException {
    String configFile = System.getProperty("bazel.configuration");
    properties = new Properties();
    try (InputStream is = new FileInputStream(configFile)) {
      properties.load(is);
    }
  }

  /** Return a file in the runfiles whose path segments are given by the arguments. */
  public static Path runfile(String... segments) {
    return Paths.get(runfileDirectory.toString(), segments);
  }

  private static void unpackBazel(String version) throws IOException, InterruptedException {
    if (!bazelVersions.containsKey(version)) {
      // Get bazel location
      Path bazelFile = runfile("build_bazel_bazel_" + version.replace('.', '_') + "/bazel");
      if (!Files.exists(bazelFile)) {
        throw new BazelWorkspaceDriverException("Bazel version " + version + " not found");
      }
      bazelVersions.put(version, bazelFile);

      // Unzip Bazel
      prepareUnpackBazelCommand(version).run();
    }
  }

  private static Command prepareUnpackBazelCommand(String version) throws IOException {
    List<String> command =
        new ArrayList<String>(
            Arrays.asList(
                bazelVersions.get(version).toString(),
                "--output_user_root=" + tmp,
                "--nomaster_bazelrc",
                "--max_idle_secs=30",
                "--bazelrc=/dev/null",
                "help"));
    return Command.builder().setDirectory(tmp).addArguments(command).build();
  }

  /** Specify with bazel version to use, required before calling bazel. */
  public void bazelVersion(String version) throws IOException, InterruptedException {
    unpackBazel(version);
    currentBazel = bazelVersions.get(version);
  }

  /** Create a new workspace, previous one can still be used. */
  public void newWorkspace() throws IOException {
    this.workspace = Files.createTempDirectory(tmp, "workspace");
    this.scratchFile("WORKSPACE");
  }

  public void setUp() throws IOException, InterruptedException {
    this.currentBazel = null;
    if (properties.get("bazel.version") != null) {
      bazelVersion(properties.getProperty("bazel.version"));
    }
    newWorkspace();
  }

  /**
   * Copy a file from the runfiles under {@code path} into {@code destpath} under the current
   * workspace.
   */
  public void copyFromRunfiles(String path, String destpath) throws IOException {
    Path origin = runfile(path);
    Path dest = createParentDirectoryIfNotExists(destpath);
    Files.copy(origin, dest);
  }

  private Path createParentDirectoryIfNotExists(String destpath) throws IOException {
    Path dest = workspace.resolve(destpath);
    Path parent = dest.getParent();
    Files.createDirectories(parent);
    return dest;
  }

  /**
   * Copy the whole directory from the runfiles under {@code directoryToCopy} to the current
   * workspace.
   */
  public void copyDirectoryFromRunfiles(final String directoryToCopy, final String stripPrefix)
      throws IOException {
    Path startingDirectory = runfile(directoryToCopy);

    if (!Files.isDirectory(startingDirectory))
      throw new BazelWorkspaceDriverException("directoryToCopy MUST be a directory");

    if (!directoryToCopy.startsWith(stripPrefix))
      throw new BazelWorkspaceDriverException(
          "The `stripPrefix` MUST be a prefix of `directoryToCopy`");

    Path stripPrefixPath = Paths.get(stripPrefix);
    try (Stream<Path> paths = Files.walk(startingDirectory)) {
      paths
          .filter(path -> Files.isRegularFile(path))
          .forEach(
              runfilePath -> {
                Path relativeToRunfilesPath = runfileDirectory.relativize(runfilePath);
                Path destinationPath = stripPrefixPath.relativize(relativeToRunfilesPath);
                try {
                  copyFromRunfiles(relativeToRunfilesPath.toString(), destinationPath.toString());
                } catch (IOException e) {
                  throw new RuntimeException(e);
                }
              });
    }
  }

  /**
   * Copy a file from the runfiles under {@code path} into {@code path} under the current workspace.
   */
  public void copyFromRunfiles(String path) throws IOException {
    copyFromRunfiles(path, path);
  }

  /**
   * Create a file under {@code path} in the current workspace, filling it with the lines given in
   * {@code content}.
   */
  public void scratchFile(String path, String... content) throws IOException {
    scratchFile(path, Arrays.asList(content));
  }

  public void scratchFile(String path, Iterable<String> content) throws IOException {
    writeToFile(path, content);
  }

  /** Append lines to a file under {@code path}, creating it if it does not exist. */
  public void appendToScratchFile(String path, String... content) throws IOException {
    appendToScratchFile(path, Arrays.asList(content));
  }

  public void appendToScratchFile(String path, Iterable<String> content) throws IOException {
    writeToFile(path, content, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
  }

  public void scratchExecutableFile(String path, String... content) throws IOException {
    scratchExecutableFile(path, Arrays.asList(content));
  }

  public void scratchExecutableFile(String path, Iterable<String> content) throws IOException {
    Path dest = writeToFile(path, content);
    dest.toFile().setExecutable(true, false);
  }

  /**
   * Scratch all the files needed for importing an executable and its runfiles.
   *
   * <p>See rule {@code bazel_executable} in {@code //tools:import.bzl}.
   */
  public ExecutableBuilder executable(String packageName, String targetName) {
    return new ExecutableBuilder(this, packageName, targetName);
  }

  private Path writeToFile(String path, Iterable<String> content, OpenOption... options)
      throws IOException {
    Path dest = createParentDirectoryIfNotExists(path);
    Files.write(
        dest, (String.join("\n", content) + "\n").getBytes(StandardCharsets.UTF_8), options);
    return dest;
  }

  private static Command prepareCommand(Path folder, Iterable<String> command) {
    return Command.builder().setDirectory(folder).addArguments(command).build();
  }

  public List<Path> workspaceDirectoryContents() {
    try {
      try (Stream<Path> files = Files.walk(workspace)) {
        return files.collect(Collectors.toList());
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static class BazelWorkspaceDriverException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private BazelWorkspaceDriverException(String message) {
      super(message);
    }
  }

  /**
   * bazel command with the given arguments. features like bazelrc file and environment can be added
   * to result object
   */
  public BazelCommand bazelCommand(String arg, String... args) {
    return bazelCommand(
        Stream.concat(Stream.of(arg), Stream.of(args)).collect(Collectors.toList()));
  }

  public BazelCommand bazelCommand(List<String> args) {
    if (currentBazel == null) {
      throw new BazelWorkspaceDriverException(
          "Cannot use bazel because no version was specified, "
              + "please call bazelVersion(version) before creating command(...).");
    }
    return new BazelCommand(args);
  }

  public class BazelCommand {

    private final List<String> args;
    private Optional<Path> bazelrcFile = Optional.empty();
    private Map<String, String> environment = new HashMap<>();
    private Path workingDirectory = Paths.get("");

    private BazelCommand(List<String> args) {
      this.args = Collections.unmodifiableList(args);
    }

    /** sets the working directory relative to workspace path */
    public BazelCommand inWorkingDirectory(Path dirRelativeToWorkspaceRoot) {
      this.workingDirectory = dirRelativeToWorkspaceRoot;
      return this;
    }

    /** sets the bazelrc file path */
    public BazelCommand withBazelrcFile(Path bazelrcFile) {
      this.bazelrcFile = Optional.of(bazelrcFile);
      return this;
    }

    /** adds environment variable to execution runtime */
    public BazelCommand withEnvironmentVariable(String key, String value) {
      this.environment.put(key, value);
      return this;
    }

    /** Prepare bazel for running, and return the {@link Command} object to run it. */
    public Command build() {
      String bazelRcPath =
          bazelrcFile.map(p -> workspace.resolve(p).toString()).orElse("/dev/null");

      List<String> command =
          new ArrayList<>(
              Arrays.asList(
                  currentBazel.toString(),
                  "--output_user_root=" + tmp,
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

      Path relativeToWorkspaceFullPath = workspace.resolve(workingDirectory);

      return Command.builder()
          .setDirectory(relativeToWorkspaceFullPath)
          .addArguments(command)
          .withEnvironment(environment)
          .build();
    }
  }
}

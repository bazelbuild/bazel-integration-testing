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
  private static Properties properties;

  private Path currentBazel = null;

  /** The current workspace. */
  private Path workspace = null;

  private static RepositoryCache repositoryCache;

  private static String javaToolchain;
  private static String javaHome;

  /** Returns the current workspace path */
  public Path currentWorkspace() {
    return workspace;
  }

  public static void setUpClass() throws IOException {
    loadProperties();
    setupTmp();
    bazelVersions = new HashMap<>();
    setupRepositoryCache();
    javaHome = javaHomeFromProperties();
    javaToolchain = javaToolchainFromProperties();
  }

  /**
   * @return The global properties passed by the starlark rule.
   * Will be null if setupClass wasn't called before calling globalBazelProperties()
   */
  static Properties globalBazelProperties() {
    return properties;
  }
  private static void setupRepositoryCache() throws IOException {
    String externalDeps = properties.getProperty("bazel.external.deps");
    repositoryCache = new RepositoryCache(tmp.resolve("cache"));
    if (externalDeps != null && !externalDeps.isEmpty()) {
      for (String dep : externalDeps.split(",")) {
        repositoryCache.put(Paths.get(dep));
      }
    }
    //freeze doesn't work on Windows
    if (OS.getCurrent() != OS.WINDOWS)
      repositoryCache.freeze();
  }

  private static void setupTmp() throws IOException {
    // We have to use a shorted output user root on Windows, otherwise we get
    // a "current working directory is too long" error,
    // so use a generated temp directory instead of "TEST_TMPDIR".
    String environmentTempDirectory =
      OS.getCurrent() == OS.WINDOWS ? null : System.getenv("TEST_TMPDIR");
    if (environmentTempDirectory == null) {
      Path tmpPath = Paths.get(System.getenv("TMP"));
      tmp = Files.createTempDirectory(tmpPath, "e4b-tests");
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
      String bazelName = "bazel" + (OS.getCurrent() == OS.WINDOWS ? ".exe" : "");
      Path bazelFile = runfile("build_bazel_bazel_" + version.replace('.', '_') + "/" + bazelName);
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

  public Path bazelBinPath() {
    if (currentBazel == null) {
      throw new IllegalStateException("bazelVersion() should have been called");
    }
    return currentBazel;
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

    // Paths.get("").relativize(Paths.get("foo/bar")) returns ..\foo\bar on Windows,
    // but it returns foo/bar on Linux.
    // Adding ./ as a prefix to all path to make sure relativize returns correct result.
    Path stripPrefixPath = Paths.get("./" + stripPrefix);
    try (Stream<Path> paths = Files.walk(startingDirectory)) {
      paths
          .filter(path -> Files.isRegularFile(path))
          .forEach(
              runfilePath -> {
                Path relativeToRunfilesPath = runfileDirectory.relativize(runfilePath);
                Path destinationPath = stripPrefixPath.relativize(Paths.get("./").resolve(relativeToRunfilesPath));
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

  @SuppressWarnings("WeakerAccess")
  public static class BazelWorkspaceDriverException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private BazelWorkspaceDriverException(String message) {
      super(message);
    }
  }

  /** Returns a builder for invoking bazel. */
  @SuppressWarnings("WeakerAccess")
  public BazelCommand.Builder bazelWithoutJavaBaseConfig(String arg, String... args) {
    return bazelWithoutJavaBaseConfig(concat(arg, args));
  }

  @SuppressWarnings("WeakerAccess")
  public BazelCommand.Builder bazelWithoutJavaBaseConfig(List<String> args) {
    return bazel(args, false);
  }

  /** Returns a builder for invoking bazel. */
  public BazelCommand.Builder bazel(String arg, String... args) {
    return bazel(concat(arg, args));
  }

  /** Returns a builder for invoking bazel. */
  public BazelCommand.Builder bazel(List<String> args) {
    return bazel(args, true);
  }

  /** Needed for custom workspace driver implementations*/
  @SuppressWarnings("WeakerAccess")
  public static Stream<String> bazelJavaFlagsForSandboxedRun() {
    return Stream.of(
        "--host_javabase=@bazel_tools//tools/jdk:absolute_javabase",
        "--java_toolchain=" + javaToolchain,
        "--define=ABSOLUTE_JAVABASE=" + javaHome
    );
  }

  private BazelCommand.Builder bazel(List<String> args, boolean addJavaBaseConfigFlags) {
    return new BazelCommand.Builder(this, tmp, repositoryCache,
        concat(args, maybeJavaBaseConfigFlags(addJavaBaseConfigFlags)));
  }

  private static List<String> concat(String arg, String[] args) {
    return Stream.concat(Stream.of(arg), Stream.of(args)).collect(Collectors.toList());
  }

  private static List<String> concat(List<String> a, List<String> b) {
    return Stream.concat(a.stream(), b.stream()).collect(Collectors.toList());
  }

  private List<String> maybeJavaBaseConfigFlags(boolean addJavaBaseConfigFlags) {
    return addJavaBaseConfigFlags ?
        bazelJavaFlagsForSandboxedRun().collect(Collectors.toList()) :
        Collections.emptyList();
  }

  private static String javaToolchainFromProperties() {
    return javaToolchainFromJavaHome(javaHome);
  }

  private static String javaHomeFromProperties() {
    return Paths.get(properties.getProperty("java_home_runfiles_path")).toAbsolutePath()
        .toString();
  }

  private static String javaToolchainFromJavaHome(String javaHome) {
    final Command build = Command.builder().addArguments(javaHome + "/bin/java", "-version").build();
    try {
      build.run();
    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
    }
    if (build.getErrorLines().stream().anyMatch(line -> line.contains("1.8."))) {
      return "@bazel_tools//tools/jdk:toolchain_hostjdk8";
    } else if (build.getErrorLines().stream().anyMatch(line -> line.contains("1.9."))) {
      return "@bazel_tools//tools/jdk:toolchain_java9";
    }  else {
      return "@bazel_tools//tools/jdk:toolchain_vanilla";
    }
  }

}

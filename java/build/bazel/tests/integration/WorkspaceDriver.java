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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WorkspaceDriver {

  private static Path tmp;
  private static Map<String, Path> bazelVersions;
  private static Path runfileDirectory = Paths.get(System.getenv("TEST_SRCDIR"));

  private Path currentBazel = null;

  /**
   * The current workspace.
   */
  private Path workspace = null;

  public static void setUpClass() throws IOException {
    String environmentTempDirectory = System.getenv("TEST_TMPDIR");
    if (environmentTempDirectory == null) {
      tmp = Files.createTempDirectory("e4b-tests");
      tmp.toFile().deleteOnExit();
    } else {
      tmp = Paths.get(environmentTempDirectory);
    }
    bazelVersions = new HashMap<>();
  }

  /**
   * Return a file in the runfiles whose path segments are given by the arguments.
   */
  public static Path getRunfile(String... segments) {
    return Paths.get(runfileDirectory.toString(), segments);
  }

  private static void unpackBazel(String version)
          throws IOException, InterruptedException {
    if (!bazelVersions.containsKey(version)) {
      // Get bazel location
      Path bazelFile = getRunfile("build_bazel_bazel_" + version.replace('.', '_') + "/bazel");
      if (!Files.exists(bazelFile)) {
        throw new BazelWorkspaceDriverException(
                "Bazel version " + version + " not found");
      }
      bazelVersions.put(version, bazelFile);

      // Unzip Bazel
      prepareUnpackBazelCommand(version).run();
    }
  }

  private static Command prepareUnpackBazelCommand(String version)
          throws IOException {
    List<String> command = new ArrayList<String>(Arrays.asList(
            bazelVersions.get(version).toString(),
            "--output_user_root=" + tmp, "--nomaster_bazelrc",
            "--max_idle_secs=30", "--bazelrc=/dev/null",
            "help"));
    return prepareCommand(tmp, Collections.unmodifiableList(command));
  }

  /**
   * Specify with bazel version to use, required before calling bazel.
   */
  public void bazelVersion(String version)
          throws IOException, InterruptedException {
    unpackBazel(version);
    currentBazel = bazelVersions.get(version);
  }

  /**
   * Create a new workspace, previous one can still be used.
   */
  public void newWorkspace() throws IOException {
    this.workspace = Files.createTempDirectory(tmp, "workspace");
    this.scratchFile("WORKSPACE");
  }

  public void setUp() throws IOException, InterruptedException {
    this.currentBazel = null;
    if (System.getProperty("bazel.version") != null) {
      bazelVersion(System.getProperty("bazel.version"));
    }
    newWorkspace();
  }

  /**
   * Prepare bazel for running, and return the {@link Command} object to run it.
   */
  public Command bazel(String... args) throws IOException {
    return bazel(new ArrayList<>(Arrays.asList(args)));
  }

  /**
   * Prepare bazel for running, and return the {@link Command} object to run it.
   */
  public Command bazel(Iterable<String> args) throws IOException {
    return runBazelInDirectory(Paths.get(""), args);
  }

  public Command runBazelInDirectory(Path relativeDir, String... args) throws IOException {
    return runBazelInDirectory(relativeDir, new ArrayList<>(Arrays.asList(args)));
  }

  public Command runBazelInDirectory(Path relativeToWorksapceDir, Iterable<String> args) throws IOException {
    if (currentBazel == null) {
      throw new BazelWorkspaceDriverException("Cannot use bazel because no version was specified, "
              + "please call bazelVersion(version) before calling bazel(...).");
    }

    List<String> command = new ArrayList<String>(Arrays.asList(
            currentBazel.toString(),
            "--output_user_root=" + tmp,
            "--nomaster_bazelrc",
            "--max_idle_secs=10",
            "--bazelrc=/dev/null"));
    for (String arg : args) {
      command.add(arg);
    }
    Path relativeToWorkspaceFullPath = workspace.resolve(relativeToWorksapceDir);

    return prepareCommand(relativeToWorkspaceFullPath, Collections.unmodifiableList(command));
  }

  /**
   * Copy a file from the runfiles under {@code path} into {@code destpath} under the current
   * workspace.
   */
  public void copyFromRunfiles(String path, String destpath) throws IOException {
    Path origin = getRunfile(path);
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
   * Copy the whole directory from the runfiles under {@code directoryToCopy} to the current workspace.
   */
  public void copyDirectoryFromRunfiles(final String directoryToCopy, final String stripPrefix) throws IOException {
    Path startingDirectory = getRunfile(directoryToCopy);

    if (!Files.isDirectory(startingDirectory))
      throw new BazelWorkspaceDriverException("directoryToCopy MUST be a directory");

    if (!directoryToCopy.startsWith(stripPrefix))
      throw new BazelWorkspaceDriverException("The `stripPrefix` MUST be a prefix of `directoryToCopy`");

    Path stripPrefixPath = Paths.get(stripPrefix);
    try (Stream<Path> paths = Files.walk(startingDirectory)) {
      paths.filter(path -> Files.isRegularFile(path))
              .forEach(runfilePath -> {
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
   * Copy a file from the runfiles under {@code path} into {@code path} under the current
   * workspace.
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

  public void scratchExecutableFile(String path, String... content) throws IOException {
    scratchExecutableFile(path, Arrays.asList(content));
  }

  public void scratchExecutableFile(String path, Iterable<String> content) throws IOException {
    File dest = writeToFile(path, content);
    dest.setExecutable(true, false);
  }

  private File writeToFile(String path, Iterable<String> content) throws IOException {
    Path dest = createParentDirectoryIfNotExists(path);
    Files.write(dest, String.join("\n", content).getBytes(StandardCharsets.UTF_8));
    return dest.toFile();
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
}

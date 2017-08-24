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
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

/** A base class to do integration test that call Bazel */
public abstract class BazelBaseTestCase {

  protected final static Joiner PATH_JOINER = Joiner.on(File.separator);
  protected final static Joiner LINE_JOINER = Joiner.on("\n");

  private static File tmp;
  private static Map<String, File> bazelVersions;
  private static File runfileDirectory = new File(System.getenv("TEST_SRCDIR"));

  private File currentBazel = null;

  /** The current workspace. */
  protected File workspace = null;

  public static class BazelTestCaseException extends Exception {
    private static final long serialVersionUID = 1L;

    private BazelTestCaseException(String message) {
      super(message);
    }
  }

  @BeforeClass
  public static void setUpClass() throws IOException {
    // Get tempdir
    String _tmp = System.getenv("TEST_TMPDIR");
    if (_tmp == null) {
      File p = Files.createTempDirectory("e4b-tests").toFile();
      p.deleteOnExit();
      tmp = p;
    } else {
      tmp = new File(_tmp);
    }
    bazelVersions = new HashMap<>();
  }

  /** Return a file in the runfiles whose path segments are given by the arguments. */
  protected static File getRunfile(String... segments) {
    return new File(PATH_JOINER.join(runfileDirectory, PATH_JOINER.join(segments)));
  }

  private static void unpackBazel(String version)
      throws BazelTestCaseException, IOException, InterruptedException {
    if (!bazelVersions.containsKey(version)) {
      // Get bazel location
      File bazelFile = getRunfile("build_bazel_bazel_" + version.replace('.', '_') + "/bazel");
      if (!bazelFile.exists()) {
        throw new BazelTestCaseException(
            "Bazel version " + version + " not found");
      }
      bazelVersions.put(version, bazelFile);
      // Unzip Bazel
      prepareCommand(tmp,
          ImmutableList.of(bazelVersions.get(version).getCanonicalPath(),
              "--output_user_root=" + tmp, "--nomaster_bazelrc",
              "--max_idle_secs=30", "--bazelrc=/dev/null", "help")).run();
    }
  }

  /** Specify with bazel version to use, required before calling bazel. */
  protected void bazelVersion(String version)
      throws BazelTestCaseException, IOException, InterruptedException {
    unpackBazel(version);
    currentBazel = bazelVersions.get(version);
  }

  /** Create a new workspace, previous one can still be used. */
  protected void newWorkspace() throws IOException {
    this.workspace = java.nio.file.Files.createTempDirectory(tmp.toPath(), "workspace").toFile();
    this.scratchFile("WORKSPACE");
  }

  @Before
  public void setUp() throws Exception {
    this.currentBazel = null;
    if (System.getProperty("bazel.version") != null) {
      bazelVersion(System.getProperty("bazel.version"));
    }
    newWorkspace();
  }

  /** Prepare bazel for running, and return the {@link Command} object to run it. */
  protected Command bazel(String... args) throws BazelTestCaseException, IOException {
    return bazel(ImmutableList.copyOf(args));
  }

  /** Prepare bazel for running, and return the {@link Command} object to run it. */
  protected Command bazel(Iterable<String> args) throws BazelTestCaseException, IOException {
    if (currentBazel == null) {
      throw new BazelTestCaseException("Cannot use bazel because no version was specified, "
          + "please call bazelVersion(version) before calling bazel(...).");
    }

    return prepareCommand(workspace,
        ImmutableList.<String>builder()
            .add(currentBazel.getCanonicalPath(), "--output_user_root=" + tmp, "--nomaster_bazelrc",
                "--max_idle_secs=10", "--bazelrc=/dev/null")
            .addAll(args).build());
  }

  /**
   * Copy a file from the runfiles under {@code path} into {@code destpath} under the current
   * workspace.
   */
  protected void copyFromRunfiles(String path, String destpath) throws IOException {
    File origin = getRunfile(path);
    File dest = new File(workspace, destpath);
    if (!dest.getParentFile().exists()) {
      dest.getParentFile().mkdirs();
    }
    Files.copy(origin.toPath(), dest.toPath());
  }

  /**
   * Copy a file from the runfiles under {@code path} into {@code path} under the current workspace.
   */
  protected void copyFromRunfiles(String path) throws IOException {
    copyFromRunfiles(path, path);
  }

  /**
   * Create a file under {@code path} in the current workspace, filling it with the lines given in
   * {@code content}.
   */
  protected void scratchFile(String path, String... content) throws IOException {
    File dest = new File(workspace,path);
    if (!dest.getParentFile().exists()) {
      dest.getParentFile().mkdirs();
    }
    Files.write(dest.toPath(), LINE_JOINER.join(content).getBytes(StandardCharsets.UTF_8));
  }

  private static Command prepareCommand(File folder, Iterable<String> command) throws IOException {
    return Command.builder().setDirectory(folder).addArguments(command).build();
  }
}

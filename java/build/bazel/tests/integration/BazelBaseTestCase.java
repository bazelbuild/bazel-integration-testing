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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import org.junit.Before;
import org.junit.BeforeClass;

/**
 * A base class to do integration test that call Bazel
 */
public abstract class BazelBaseTestCase {

  protected WorkspaceDriver driver = new WorkspaceDriver();

  @BeforeClass
  public static void setUpClass() throws IOException {
    WorkspaceDriver.setUpClass();
  }

  /**
   * Return a file in the runfiles whose path segments are given by the arguments.
   */
  protected static File getRunfile(String... segments) {
    return WorkspaceDriver.getRunfile(segments);
  }

  /**
   * Specify with bazel version to use, required before calling bazel.
   */
  protected void bazelVersion(String version) throws WorkspaceDriver.BazelWorkspaceDriverException, IOException, InterruptedException {
    driver.bazelVersion(version);
  }

  /**
   * Create a new workspace, previous one can still be used.
   */
  protected void newWorkspace() throws IOException {
    driver.newWorkspace();
  }

  @Before
  public void setUp() throws Exception {
    driver.setUp();
  }

  /**
   * Prepare bazel for running, and return the {@link Command} object to run it.
   */
  protected Command bazel(String... args) throws WorkspaceDriver.BazelWorkspaceDriverException, IOException {
    return driver.bazel(args);
  }

  /**
   * Prepare bazel for running, and return the {@link Command} object to run it.
   */
  protected Command bazel(Iterable<String> args) throws WorkspaceDriver.BazelWorkspaceDriverException, IOException {
    return driver.bazel(args);
  }

  /**
   * Copy a file from the runfiles under {@code path} into {@code destpath} under the current
   * workspace.
   */
  protected void copyFromRunfiles(String path, String destpath) throws IOException {
    driver.copyFromRunfiles(path, destpath);
  }

  /**
   * Copy a file from the runfiles under {@code path} into {@code path} under the current
   * workspace.
   */
  protected void copyFromRunfiles(String path) throws IOException {
    driver.copyFromRunfiles(path);
  }

  /**
   * Create a file under {@code path} in the current workspace, filling it with the lines given in
   * {@code content}.
   */
  protected void scratchFile(String path, String... content) throws IOException {
    driver.scratchFile(path, content);
  }

  protected void scratchFile(String path, Iterable<String> content) throws IOException {
    driver.scratchFile(path, content);
  }

  protected void scratchExecutableFile(String path, String... content) throws IOException {
    driver.scratchExecutableFile(path, content);
  }

  protected void scratchExecutableFile(String path, Iterable<String> content) throws IOException {
    driver.scratchExecutableFile(path, content);
  }

  protected List<String> workspaceContents() {
    return driver.contents();
  }
}

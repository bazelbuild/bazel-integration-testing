package build.bazel.tests.integration;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static com.spotify.hamcrest.optional.OptionalMatchers.optionalWithValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.StringEndsWith.endsWith;

public class WorkspaceDriverTest {
  private WorkspaceDriver driver = new WorkspaceDriver();

  @BeforeClass
  public static void setUpClass() throws IOException {
    WorkspaceDriver.setUpClass();
  }

  @Before
  public void setUp() throws Exception {
    driver.setUp();
  }

  @Test
  public void copyDirectoryFromRunfilesWithoutPrefixShouldCopyTheWholeDirectory() throws IOException, WorkspaceDriver.BazelWorkspaceDriverException {
    String knownFile = "bazel_tools/tools/jdk/TestRunner_deploy.jar";

    driver.copyDirectoryFromRunfiles("bazel_tools/tools", "");

    Optional<String> actualFilePath = findPath(driver.contents(), knownFile);
    org.hamcrest.MatcherAssert.assertThat("the known file should be found in the workspace", actualFilePath, is(optionalWithValue(is(endsWith(knownFile)))));
  }

  @Test
  public void copyDirectoryFromRunfilesShouldCopyTheWholeDirectoryAndStripPrefix() throws IOException, WorkspaceDriver.BazelWorkspaceDriverException {
    String sourceDirectoryThatWillBeStripped = "bazel_tools/tools";
    String theFileToCopy = "/jdk/TestRunner_deploy.jar";
    String knownFile = sourceDirectoryThatWillBeStripped + theFileToCopy;

    driver.copyDirectoryFromRunfiles(sourceDirectoryThatWillBeStripped, sourceDirectoryThatWillBeStripped);

    Optional<String> actualFilePath = findPath(driver.contents(), theFileToCopy);
    org.hamcrest.MatcherAssert.assertThat("the known file should be found in the workspace", actualFilePath, is(optionalWithValue(is(endsWith(theFileToCopy)))));
    org.hamcrest.MatcherAssert.assertThat("the root path from the runfiles should be stripped", actualFilePath, is(optionalWithValue(not(endsWith(knownFile)))));
  }

  @Test(expected = WorkspaceDriver.BazelWorkspaceDriverException.class)
  public void copyDirectoryFromRunfilesShouldThrowIfNotDirectory() throws IOException, WorkspaceDriver.BazelWorkspaceDriverException {
    String knownFile = "bazel_tools/tools/jdk/TestRunner_deploy.jar";

    driver.copyDirectoryFromRunfiles(knownFile, "");
  }

  @Test(expected = WorkspaceDriver.BazelWorkspaceDriverException.class)
  public void copyDirectoryFromRunfilesShouldThrowIfTheStripPrefixIsntAPrefix() throws IOException, WorkspaceDriver.BazelWorkspaceDriverException {
    String knownFile = "bazel_tools/tools/jdk/TestRunner_deploy.jar";

    driver.copyDirectoryFromRunfiles(knownFile, "blabla");
  }

  private Optional<String> findPath(List<String> paths, String path) {
    return paths.stream().filter(x -> x.endsWith(path)).findFirst();
  }
}

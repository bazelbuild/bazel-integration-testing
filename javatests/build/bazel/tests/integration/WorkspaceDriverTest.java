package build.bazel.tests.integration;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import static com.spotify.hamcrest.optional.OptionalMatchers.emptyOptional;
import static com.spotify.hamcrest.optional.OptionalMatchers.optionalWithValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.StringEndsWith.endsWith;
import static org.junit.Assert.assertTrue;

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
  public void copyDirectoryFromRunfilesWithoutPrefixShouldCopyTheWholeDirectory() throws IOException {
    String knownFile = "bazel_tools/tools/jdk/TestRunner_deploy.jar";

    driver.copyDirectoryFromRunfiles("bazel_tools/tools", "");

    Optional<String> actualFilePath = findPath(driver.contents(), knownFile);
    org.hamcrest.MatcherAssert.assertThat("the known file should be found in the workspace", actualFilePath, is(optionalWithValue(is(endsWith(knownFile)))));
  }

  @Test
  public void copyDirectoryFromRunfilesShouldCopyTheWholeDirectoryAndStripPrefix() throws IOException {
    String sourceDirectoryThatWillBeStripped = "bazel_tools/tools";
    String theFileToCopy = "/jdk/TestRunner_deploy.jar";
    String knownFile = sourceDirectoryThatWillBeStripped + theFileToCopy;

    driver.copyDirectoryFromRunfiles(sourceDirectoryThatWillBeStripped, sourceDirectoryThatWillBeStripped);

    Optional<String> actualFilePath = findPath(driver.contents(), theFileToCopy);
    org.hamcrest.MatcherAssert.assertThat("the known file should be found in the workspace", actualFilePath, is(optionalWithValue(is(endsWith(theFileToCopy)))));
    org.hamcrest.MatcherAssert.assertThat("the root path from the runfiles should be stripped", actualFilePath, is(optionalWithValue(not(endsWith(knownFile)))));
  }

  @Test(expected = WorkspaceDriver.BazelWorkspaceDriverException.class)
  public void copyDirectoryFromRunfilesShouldThrowIfNotDirectory() throws IOException {
    String knownFile = "bazel_tools/tools/jdk/TestRunner_deploy.jar";

    driver.copyDirectoryFromRunfiles(knownFile, "");
  }

  @Test(expected = WorkspaceDriver.BazelWorkspaceDriverException.class)
  public void copyDirectoryFromRunfilesShouldThrowIfTheStripPrefixIsntAPrefix() throws IOException {
    String knownFile = "bazel_tools/tools/jdk/TestRunner_deploy.jar";

    driver.copyDirectoryFromRunfiles(knownFile, "blabla");
  }

  @Test
  public void scratchFileShouldCreateFileAndWorkspaceContentsContainThatFile() throws IOException {
    String content = "somecontent";
    String path = "somePath";

    driver.scratchFile(path, content);

    Optional<String> actualScratchFileContent = findPath(driver.contents(), path).map(this::readFileContent);
    org.hamcrest.MatcherAssert.assertThat(actualScratchFileContent, is(optionalWithValue(equalTo(content))));
  }

  @Test
  public void scratchExecutableFileShouldCreateAnExecutableFile() throws IOException {
    String path = "someExecutablePath";

    driver.scratchExecutableFile(path);

    Optional<Boolean> isExecutable = findPath(driver.contents(), path).map(this::isExecutable);
    org.hamcrest.MatcherAssert.assertThat("The file should be executable", isExecutable, is(optionalWithValue(equalTo(true))));
  }

  @Test
  public void getRunfileReturnTheFile() {
    File runfile = WorkspaceDriver.getRunfile("bazel_tools", "tools", "jdk", "TestRunner_deploy.jar");

    assertTrue("runfile should exists", runfile.exists());
  }

  @Test
  public void newWorkspaceCreatesANewCleanWorkspace() throws IOException {
    String path = "somePathForNewWorkspace";
    driver.scratchFile(path);

    driver.newWorkspace();

    Optional<String> fullPath = findPath(driver.contents(), path);
    org.hamcrest.MatcherAssert.assertThat("Workspace should be cleaned",fullPath, is(emptyOptional()));
  }

  private Boolean isExecutable(String path) {
    return Files.isExecutable(Paths.get(path));
  }

  private String readFileContent(String path) {
    try {
      return new String(Files.readAllBytes(Paths.get(path)));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private Optional<String> findPath(List<String> paths, String path) {
    return paths.stream().filter(x -> x.endsWith(path)).findFirst();
  }
}

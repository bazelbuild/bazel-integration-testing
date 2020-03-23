package build.bazel.tests.integration;

import static com.spotify.hamcrest.optional.OptionalMatchers.emptyOptional;
import static com.spotify.hamcrest.optional.OptionalMatchers.optionalWithValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.StringEndsWith.endsWith;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

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
  public void copyDirectoryFromRunfilesWithoutPrefixShouldCopyTheWholeDirectory()
      throws IOException {
    String knownFile = "build_bazel_integration_testing/javatests/build/bazel/tests/integration/WorkspaceDriverTest/" + jarNameAccordingToCurrentBazelVersion();

    driver.copyDirectoryFromRunfiles("build_bazel_integration_testing/javatests/build/bazel/tests/integration", "");

    Optional<Path> actualFilePath = findPath(driver.workspaceDirectoryContents(), knownFile);
    org.hamcrest.MatcherAssert.assertThat(
        "the known file should be found in the workspace", actualFilePath, is(optionalWithValue()));
  }

  // Test that we get the actual workspace with currentWorkspace()
  @Test
  public void fileExistsUnderWorkspacePath() throws IOException {
    driver.scratchFile("test_workspace_file");
    org.hamcrest.MatcherAssert.assertThat(
        "test_workspace_file should be in the workspace",
        new File(driver.currentWorkspace().toFile(), "test_workspace_file").exists(),
        is(true));
  }

  @Test
  public void copyDirectoryFromRunfilesShouldCopyTheWholeDirectoryAndStripPrefix()
      throws IOException {
    String sourceDirectoryThatWillBeStripped = "build_bazel_integration_testing/javatests/build/bazel/tests/integration";
    String theFileToCopy = "/WorkspaceDriverTest/" + jarNameAccordingToCurrentBazelVersion();
    String knownFile = sourceDirectoryThatWillBeStripped + theFileToCopy;

    driver.copyDirectoryFromRunfiles(
        sourceDirectoryThatWillBeStripped, sourceDirectoryThatWillBeStripped);

    Optional<Path> actualFilePath = findPath(driver.workspaceDirectoryContents(), theFileToCopy);
    org.hamcrest.MatcherAssert.assertThat(
        "the known file should be found in the workspace", actualFilePath, is(optionalWithValue()));
    org.hamcrest.MatcherAssert.assertThat(
        "the root path from the runfiles should be stripped",
        actualFilePath.map(Path::toString),
        is(not(optionalWithValue(endsWith(knownFile)))));
  }

  @Test(expected = WorkspaceDriver.BazelWorkspaceDriverException.class)
  public void copyDirectoryFromRunfilesShouldThrowIfNotDirectory() throws IOException {
    String knownFile = "build_bazel_integration_testing/javatests/build/bazel/tests/integration/WorkspaceDriverTest/" + jarNameAccordingToCurrentBazelVersion();

    driver.copyDirectoryFromRunfiles(knownFile, "");
  }

  @Test(expected = WorkspaceDriver.BazelWorkspaceDriverException.class)
  public void copyDirectoryFromRunfilesShouldThrowIfTheStripPrefixIsntAPrefix() throws IOException {
    String knownFile = "build_bazel_integration_testing/javatests/build/bazel/tests/integration/WorkspaceDriverTest/"  + jarNameAccordingToCurrentBazelVersion();

    driver.copyDirectoryFromRunfiles(knownFile, "blabla");
  }

  @Test
  public void scratchFileShouldCreateFileAndWorkspaceContentsContainThatFile() throws IOException {
    String content = "somecontent";
    String path = "somePath";

    driver.scratchFile(path, content);

    Optional<String> actualScratchFileContent =
        findPath(driver.workspaceDirectoryContents(), path).map(this::readFileContent);
    org.hamcrest.MatcherAssert.assertThat(
        actualScratchFileContent, is(optionalWithValue(equalTo(content + "\n"))));
  }

  @Test
  public void appendToScratchFileShouldCreateFileAndWorkspaceContentsContainThatFile()
      throws IOException {
    String content = "somecontent";
    String path = "somePath";

    driver.appendToScratchFile(path, content);

    Optional<String> actualScratchFileContent =
        findPath(driver.workspaceDirectoryContents(), path).map(this::readFileContent);
    org.hamcrest.MatcherAssert.assertThat(
        actualScratchFileContent, is(optionalWithValue(equalTo(content + "\n"))));

    driver.appendToScratchFile(path, "foobar");
    actualScratchFileContent =
        findPath(driver.workspaceDirectoryContents(), path).map(this::readFileContent);
    org.hamcrest.MatcherAssert.assertThat(
        actualScratchFileContent, is(optionalWithValue(equalTo(content + "\nfoobar\n"))));
  }

  @Test
  public void scratchExecutableFileShouldCreateAnExecutableFile() throws IOException {
    String path = "someExecutablePath";

    driver.scratchExecutableFile(path);

    Optional<Boolean> isExecutable =
        findPath(driver.workspaceDirectoryContents(), path).map(this::isExecutable);
    org.hamcrest.MatcherAssert.assertThat(
        "The file should be executable", isExecutable, is(optionalWithValue(equalTo(true))));
  }

  @Test
  public void runfileReturnTheFile() {
    Path runfile = WorkspaceDriver.runfile("build_bazel_integration_testing",
        "javatests",
        "build",
        "bazel",
        "tests",
        "integration",
        "WorkspaceDriverTest",
        jarNameAccordingToCurrentBazelVersion());

    assertTrue("runfile should exists", Files.exists(runfile));
  }

  @Test
  public void newWorkspaceCreatesANewCleanWorkspace() throws IOException {
    String path = "somePathForNewWorkspace";
    driver.scratchFile(path);

    driver.newWorkspace();

    Optional<Path> fullPath = findPath(driver.workspaceDirectoryContents(), path);
    org.hamcrest.MatcherAssert.assertThat(
        "Workspace should be cleaned", fullPath, is(emptyOptional()));
  }

  private Boolean isExecutable(Path path) {
    return Files.isExecutable(path);
  }

  private String readFileContent(Path path) {
    try {
      return new String(Files.readAllBytes(path));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private Optional<Path> findPath(List<Path> paths, String path) {
    final String path_platform = (OS.getCurrent() == OS.WINDOWS) ? path.replace("/", "\\") : path;
    return paths.stream().filter(x -> x.toString().endsWith(path_platform)).findFirst();
  }

  private String jarNameAccordingToCurrentBazelVersion() {
    return "bazel" + WorkspaceDriver.globalBazelProperties().getProperty("bazel.version") + ".jar";
  }

}

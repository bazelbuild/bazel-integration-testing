package build.bazel.tests.integration;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

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

  @Test(expected = WorkspaceDriver.BazelWorkspaceDriverException.class)
  public void copyDirectoryFromRunfilesShouldThrowIfNotDirectory() throws IOException, WorkspaceDriver.BazelWorkspaceDriverException {
    String knownFile = "bazel_tools/tools/jdk/TestRunner_deploy.jar";

    driver.copyDirectoryFromRunfiles(knownFile);
  }
}

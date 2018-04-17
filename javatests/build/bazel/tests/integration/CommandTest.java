package build.bazel.tests.integration;

import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import static org.hamcrest.core.IsCollectionContaining.hasItem;

public class CommandTest {

    @Test
    public void runWithEnvironment() throws IOException, InterruptedException {
        String key = "MY_KEY";
        String value = "some-value";
        Map<String, String> environment = Collections.singletonMap(key, value);
        Command command = Command.builder().addArguments("bash", "-c", "echo ${" + key + "}").build();

        command.run(environment);

        org.hamcrest.MatcherAssert.assertThat(command.getOutputLines(), hasItem(value));
    }
}

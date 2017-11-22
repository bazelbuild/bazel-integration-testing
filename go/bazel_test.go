package external_test

import (
	"runtime/debug"
	"strings"
	"testing"

	bazel "github.com/bazelbuild/bazel-integration-testing/go"
)

func must(t *testing.T, err error) {
	if err != nil {
		t.Errorf("Error: %s", err)
		debug.PrintStack()
	}
}

func TestVersion(t *testing.T) {
	if strings.Contains(bazel.BazelVersion, "error") {
		t.Fatalf("Something is wrong with the compile and the BazelVersion variable wasn't set. This is a fatal error. Expected it to not contain error, but it was [%s]", bazel.BazelVersion)
	}

	b, err := bazel.New()
	if err != nil {
		t.Fatal(err)
	}

	must(t, b.ScratchFile("WORKSPACE", ""))
	must(t, b.ScratchDir("demo/testing"))
	exitCode, stdout, stderr := b.RunBazel([]string{"info", "release"})

	if exitCode != 0 {
		t.Errorf("Error code expected 0. Was: %d.\nStdout:\n%s\nStderr:\n%s", exitCode, stdout, stderr)
	}
	if !strings.Contains(stdout, "release "+bazel.BazelVersion) {
		t.Errorf("Stdout didn't contain version (%s). Stdout:\n[%s]", bazel.BazelVersion, stdout)
	}
}

func TestSimpleShellscript(t *testing.T) {
	b, err := bazel.New()
	if err != nil {
		t.Fatal(err)
	}

	must(t, b.ScratchFile("WORKSPACE", ""))
	must(t, b.ScratchDir("demo"))
	must(t, b.ScratchDir("demo/testing"))
	must(t, b.ScratchFile("demo/testing/BUILD", `sh_binary(name="testing", srcs=["test.sh"])`))
	must(t, b.ScratchFileWithMode("demo/testing/test.sh", `echo "Success!"`, 0755))

	exitCode, stdout, stderr := b.RunBazel([]string{"run", "//demo/testing"})

	if exitCode != 0 {
		t.Errorf("Error code expected 0. Was: %d.\nStdout:\n%s\nStderr:\n%s", exitCode, stdout, stderr)
	}
	if !strings.Contains(stdout, "Success!") {
		t.Errorf("Stdout didn't contain [Success!].\nStdout: [%s]", bazel.BazelVersion, stdout)
	}
}

load("//:bazel_integration_test.bzl", "bazel_java_integration_test")

bazel_java_integration_test(
    name = "BazelBaseTestCaseTest",
    srcs = ["BazelBaseTestCaseTest.java"],
    data = [
        "//:all_bzl",
    ],
    jvm_flags = ["-Dfoo.bar=true"],
    deps = [
        "//java/build/bazel/tests/integration",
        "@com_google_truth//jar",
    ],
    # We had to disable this test on Windows because it will fail due to running Bazel
    # from a path longer than 256 charactors.
    tags = ["no_windows"],
)

bazel_java_integration_test(
    name = "WorkspaceDriverTest",
    srcs = ["WorkspaceDriverTest.java"],
    deps = ["@com_spotify_hamcrest_optional//jar"],
)

java_library(
    name = "BazelIntegrationTestNoSourcesSrc",
    srcs = ["BazelIntegrationTestNoSourcesTest.java"],
    deps = ["@org_junit//jar"],
)

# The below uses test_class instead of renaming the BazelIntegrationTestNoSourcesSrc target.
# This demonstrates a usage of test_class attribute.
bazel_java_integration_test(
    name = "BazelIntegrationTestNoSources",
    test_class = "build.bazel.tests.integration.BazelIntegrationTestNoSourcesTest",
    runtime_deps = [":BazelIntegrationTestNoSourcesSrc"],
)

bazel_java_integration_test(
    name = "WorkspaceDriverIntegrationTest",
    srcs = ["WorkspaceDriverIntegrationTest.java"],
    external_deps = [
        "@test_archive",
        "@test_archive2",
    ],
    # Ensure that there could be no network fetch in sandboxing.  This tag is a Bazel
    # built-in, it is important to keep it.
    tags = [
        "block-network",
        "exclusive",
    ],
    deps = ["//java/build/bazel/tests/integration"],
)

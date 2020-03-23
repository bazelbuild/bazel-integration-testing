load("//:bazel_integration_test.bzl", "bazel_java_integration_test")
load("//tools:common.bzl", "GET_LATEST_BAZEL_VERSIONS")

# We only run BazelBaseTestCaseTest with Bazel version >= 0.21.0,
# where --enable_runfiles is supported.
bazel_java_integration_test(
    name = "BazelBaseTestCaseTest",
    srcs = ["BazelBaseTestCaseTest.java"],
    data = [
        "//:all_bzl",
    ],
    jvm_flags = ["-Dfoo.bar=true"],
    versions = GET_LATEST_BAZEL_VERSIONS(),
    deps = [
        "//java/build/bazel/tests/integration",
        "@com_google_truth//jar",
    ],
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
    data = ["//testdata:java_executable_with_runfiles"],
    external_deps = [
        "@test_archive",
        "@test_archive2",
    ],
    # Ensure that there could be no network fetch in sandboxing.  This tag is a Bazel
    # built-in, it is important to keep it.
    tags = ["block-network"],
    deps = ["//java/build/bazel/tests/integration"],
)

bazel_java_integration_test(
    name = "RBEExampleTest",
    srcs = ["RBEExampleTest.java"],
    deps = ["//java/build/bazel/tests/integration"],
    versions = [
        "0.22.0",
        "0.23.2",
    ],
    external_deps = [
        "@bazel_toolchains_test",
    ],
)

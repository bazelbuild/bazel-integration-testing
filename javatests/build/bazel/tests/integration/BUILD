load("//:bazel_integration_test.bzl", "bazel_java_integration_test")
load("//tools:common.bzl", "GET_LATEST_BAZEL_VERSIONS")
load("//tools:common.bzl", "BAZEL_VERSIONS")

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
    # Some older version doesn't support --enable_runfiles on Windows
    tags = ["no_windows"],
)

# We only run BazelBaseTestCaseTest with Bazel version >= 0.21.0,
# where --enable_runfiles is supported.
# Unfortunately, versions attribute doesn't support select, so we have
# to create a separate target for Windows.
bazel_java_integration_test(
    name = "BazelBaseTestCaseTest_windows",
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
    # No need to rerun the same tests on Linux and macOs
    tags = [
        "no_macos",
        "no_linux",
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
    external_deps = [
        "@test_archive",
        "@test_archive2",
    ],
    deps = ["//java/build/bazel/tests/integration"],
)

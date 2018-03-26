load("//tools:common.bzl", "BAZEL_VERSIONS")

java_library(
    name = "workspace_driver",
    srcs = ["WorkspaceDriver.java",
            "Command.java",
            "LineListOutputStream.java",],
    testonly = True,
    visibility = ["//visibility:public"]
)

java_library(
    name = "integration",
    srcs = [
        "BazelBaseTestCase.java",
    ],
    deps = [
        ":workspace_driver",
        "@com_google_guava//jar",
        "@org_junit//jar",
    ],
    testonly = True,
    visibility = ["//visibility:public"]
)

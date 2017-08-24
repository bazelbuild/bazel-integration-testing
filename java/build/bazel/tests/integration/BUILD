load("//tools:common.bzl", "BAZEL_VERSIONS")

java_library(
    name = "integration",
    srcs = [
        "BazelBaseTestCase.java",
        "Command.java",
        "LineListOutputStream.java",
    ],
    data = [
        "@build_bazel_bazel_%s//:bazel_binary" % v.replace(".", "_")
        for v in BAZEL_VERSIONS
    ],
    deps = [
        "@com_google_guava//jar",
        "@org_junit//jar",
    ],
    testonly = True,
    visibility = ["//visibility:public"]
)

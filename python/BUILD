load("@build_bazel_integration_testing//tools:common.bzl", "BAZEL_VERSIONS")
load("@build_bazel_integration_testing//python:python.bzl", "bazel_py_integration_test")
load("@bazel_skylib//:bzl_library.bzl", "bzl_library")

bzl_library(
    name = "bzl",
    srcs = ["python.bzl"],
    visibility = ["//:internal"],
)

py_library(
    name = "python",
    testonly = True,
    srcs = ["test_base.py"],
    visibility = ["//visibility:public"],
)

[
    genrule(
        name = "python_generated_version_src_" + v,
        srcs = [],
        outs = [v + "/bazel_py_integration_test/generated/__init__.py"],
        cmd = "echo 'DEFAULT_BAZEL_VERSION = \"%s\"' >$@ " % v,
    )
    for v in BAZEL_VERSIONS
]

[
    py_library(
        name = "python_version_" + v,
        srcs = [":python_generated_version_src_" + v],
        data = ["@build_bazel_bazel_%s//:bazel_binary" % v.replace(".", "_")],
        imports = [v],
        visibility = ["//visibility:public"],
    )
    for v in BAZEL_VERSIONS
]

bazel_py_integration_test(
    name = "test_base_test",
    srcs = ["test_base_test.py"],
)

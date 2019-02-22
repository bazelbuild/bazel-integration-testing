package(default_visibility = ["//visibility:public"])

load("@bazel_skylib//:bzl_library.bzl", "bzl_library")

bzl_library(
    name = "bzl",
    srcs = glob(["*.bzl"]),
    visibility = ["//:internal"],
)

py_binary(
    name = "create_bazel_hashdict",
    srcs = ["create_bazel_hashdict.py"],
    deps = ["@com_google_python_gflags//:gflags"],
)

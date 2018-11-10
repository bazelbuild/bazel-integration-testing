package(default_visibility = ["//visibility:public"])

py_binary(
    name = "create_bazel_version_dict",
    srcs = ["create_bazel_version_dict.py"],
    deps = ["@com_google_python_gflags//:gflags"],
)

filegroup(
    name = "tools",
    srcs = glob(["*"]),
    visibility = ["//:internal"],
)

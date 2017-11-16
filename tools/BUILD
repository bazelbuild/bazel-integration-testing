package(default_visibility = ["//visibility:public"])

py_binary(
    name = "create_bazel_hashdict",
    srcs = ["create_bazel_hashdict.py"],
    deps = ["@com_google_python_gflags//:gflags"],
)
filegroup(
    name = "tools",
    srcs = glob(["*"])
)

package(
    default_testonly = True,
    default_visibility = ["//javatests:__subpackages__"],
)

load("//tools:import.bzl", "bazel_executable")

java_library(
    name = "java_lib",
    srcs = ["Executable.java"],
    data = ["data.txt"],
    deps = [
        "@org_junit//jar",
    ],
)

java_binary(
    name = "java_executable",
    main_class = "testdata.Executable",
    # We use runtime_deps to showcase transitive runfile collection
    runtime_deps = ["java_lib"],
)

bazel_executable(
    name = "java_executable_with_runfiles",
    executable = "java_executable",
)

workspace(name = "build_bazel_integration_testing")

load("//:bazel_version.bzl", "check_bazel_version")
load("//:bazel_integration_test.bzl", "bazel_java_integration_test_deps")

check_bazel_version("0.5.0")

bazel_java_integration_test_deps()

new_http_archive(
    name = "com_google_python_gflags",
    url = "https://github.com/google/python-gflags/archive/python-gflags-2.0.zip",
    strip_prefix = "python-gflags-python-gflags-2.0",
    build_file_content = """
py_library(
    name = "gflags",
    srcs = [
        "gflags.py",
        "gflags_validators.py",
    ],
    visibility = ["//visibility:public"],
)
""",
    sha256 = "344990e63d49b9b7a829aec37d5981d558fea12879f673ee7d25d2a109eb30ce",
)

maven_jar(
    name = "com_google_truth",
    artifact = "com.google.truth:truth:jar:0.31",
)

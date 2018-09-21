# Copyright 2018 The Bazel Authors. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
""" Dependencies for linting/formatting.
"""

load(
    "@bazel_tools//tools/build_defs/repo:http.bzl",
    "http_archive",
    "http_file",
)
load(
    "@bazel_tools//tools/build_defs/repo:java.bzl",
    "java_import_external",
)

def _com_github_google_yapf_repository_impl(rctx):
  rctx.download_and_extract(
      url = "https://github.com/google/yapf/archive/v0.21.0.tar.gz",
      sha256 = "b930c1bc8233a9944671db7bdd6c9dc9ba2343b08b726a2dd0bff37ce1815baa",
      stripPrefix = "yapf-0.21.0")
  rctx.file("BUILD", """
alias(
    name="yapf",
    actual="//yapf:yapf",
    visibility = ["//visibility:public"],
)
""")
  rctx.file("yapf/BUILD", """
py_binary(
    name="yapf",
    srcs=glob(["**/*.py"]),
    main="__main__.py",
    visibility = ["//visibility:public"],
)""")

_com_github_google_yapf_repository = repository_rule(
    attrs = {},
    implementation = _com_github_google_yapf_repository_impl,
)

def format_repositories():
  _com_github_google_yapf_repository(name = "com_github_google_yapf")

  http_archive(
      name = "io_bazel",
      urls = [
          "https://github.com/bazelbuild/bazel/releases/download/0.17.1/bazel-0.17.1-dist.zip"
      ],
      sha256 = (
          "23e4281c3628cbd746da3f51330109bbf69780bd64461b63b386efae37203f20"),
  )

  java_import_external(
      name = "google_java_format",
      licenses = ["notice"],  # Apache 2.0
      jar_urls = [
          "https://github.com/google/google-java-format/releases/download/google-java-format-1.5/google-java-format-1.5-all-deps.jar"
      ],
      jar_sha256 = ("7b839bb7534a173f0ed0cd0e9a583181d20850fcec8cf6e3800e4420a1fad184"),
  )

  http_file(
      name = "io_bazel_buildifier_linux",
      urls = [
          "https://github.com/bazelbuild/buildtools/releases/download/0.15.0/buildifier"
      ],
      sha256 = (
          "0dea01a7a511797878f486e6ed8e549980c0710a0a116c8ee953d4e26de41515"),
      executable = True,
  )

  http_file(
      name = "io_bazel_buildifier_darwin",
      urls = [
          "https://github.com/bazelbuild/buildtools/releases/download/0.15.0/buildifier.osx"
      ],
      sha256 = (
          "860378a2badba9517e523e20f152ef1ca16234e0ca462a1d71e5dbee7d506771"),
      executable = True,
  )

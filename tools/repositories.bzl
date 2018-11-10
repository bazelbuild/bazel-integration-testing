# Copyright 2017 The Bazel Authors. All rights reserved.
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

load(":common.bzl", "BAZEL_VERSION_DICT", "BAZEL_VERSIONS")

_BAZEL_BINARY_BUILD = {
  "default": """
filegroup(
  name = "bazel_binary",
  srcs = ["bazel-real","bazel"],
  visibility = ["//visibility:public"])""",
  "windows-x86_64": """
filegroup(
  name = "bazel_binary",
  srcs = ["bazel.exe"],
  visibility = ["//visibility:public"])""",
}

def _get_platform_name(rctx):
  os_name = rctx.os.name.lower()
  if os_name.startswith("mac os"):
    return "darwin-x86_64"
  elif os_name.startswith("windows"):
    return "windows-x86_64"
  else:
    return "linux-x86_64"

def _get_installer(rctx):
  platform = _get_platform_name(rctx)
  version = rctx.attr.version
  meta = BAZEL_VERSION_DICT[version][platform]
  rctx.download_and_extract(type="zip", **meta)

def _bazel_repository_impl(rctx):
  _get_installer(rctx)
  platform = _get_platform_name(rctx)
  rctx.file("WORKSPACE", "workspace(name='%s')" % rctx.attr.name)
  rctx.file("BUILD", _BAZEL_BINARY_BUILD.get(platform,
                                             _BAZEL_BINARY_BUILD["default"]))

bazel_binary = repository_rule(
    attrs = {
        "version": attr.string(default = "0.5.3"),
    },
    implementation = _bazel_repository_impl,
)
"""Download a bazel binary for integration test.

Args:
  version: the version of Bazel to download.
"""

def bazel_binaries(versions = BAZEL_VERSIONS):
    """Download all bazel binaries specified in BAZEL_VERSIONS."""
    for version in versions:
        name = "build_bazel_bazel_" + version.replace(".", "_")
        if not native.existing_rule(name):
            bazel_binary(name = name, version = version)

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

# Python integration test framework for using Bazel
load("//tools:common.bzl", "BAZEL_VERSIONS")
load("//tools:repositories.bzl", "bazel_binaries")
load("@io_bazel_rules_go//go:def.bzl", "go_test")

def _make_compatible_version_list():
  toReturn = []
  for v in BAZEL_VERSIONS:
    major, minor, patch = v.split(".")
    if int(major) == 0 and int(minor) >= 7:
      toReturn.append("%s.%s.%s" % (major, minor, patch))
  return toReturn

RULES_GO_COMPATIBLE_BAZEL_VERSION = _make_compatible_version_list()

def bazel_go_integration_test(name,
                              srcs,
                              deps = [],
                              data = [],
                              versions = RULES_GO_COMPATIBLE_BAZEL_VERSION,
                              **kwargs):
  """A wrapper around go_test that create several go tests, one per version
     of Bazel.

     Args:
       versions: list of version of bazel to create a test for. Each test
         will be named `<name>/bazel<version>`.
       See go_test for the other arguments.
  """
  for version in versions:
    go_test(
        name = "%s/bazel%s" % (name, version),
        srcs = srcs,
        deps = deps,
        data = [
            "@build_bazel_bazel_%s//:bazel_binary" % version.replace(".", "_"),
        ] + data,
        x_defs = {
            "github.com/bazelbuild/bazel-integration-testing/go.BazelVersion": version,
        },
        **kwargs)

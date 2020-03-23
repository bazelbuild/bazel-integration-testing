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

# Java integration test framework for using Bazel
load(
    ":common.bzl",
    "BAZEL_VERSIONS",
)
load(
    ":repositories.bzl",
    "bazel_binaries",
)
load(
    "@bazel_tools//tools/build_defs/repo:java.bzl",
    "java_import_external",
)

load("@bazel_tools//tools/build_defs/repo:jvm.bzl", "jvm_maven_import_external")

def _bazel_java_integration_test_properties_impl(ctx):
  java_runtime_info = ctx.attr._java_runtime[java_common.JavaRuntimeInfo]
  properties = [
      "bazel.version=" + ctx.attr.bazel_version,
      "bazel.workspace=" + ctx.workspace_name,
      "bazel.external.deps=" +
      ",".join([d.short_path for d in ctx.files.external_deps]),
      "java_home_runfiles_path=" + "%s" % java_runtime_info.java_home_runfiles_path,
  ]

  ctx.actions.write(ctx.outputs.properties, "\n".join(properties))

bazel_java_integration_test_properties_ = rule(
    attrs = {
        "bazel_version": attr.string(mandatory = True),
        "external_deps": attr.label_list(allow_files = True),
        "_java_runtime": attr.label(
            default = Label("@bazel_tools//tools/jdk:current_java_runtime"),
        ),
    },
    implementation = _bazel_java_integration_test_properties_impl,
    outputs = {
        "properties": "%{name}.properties",
    },
)

def _index(lst, el):
  return lst.index(el) if el in lst else -1

def _java_package():
  # Adaptation of the java class finding library from Bazel.
  path = native.package_name()
  segments = path.split("/")
  roots = [
      segments.index(i) for i in ["java", "javatests", "src"] if i in segments
  ]
  if not len(roots):
    return ".".join(segments)
  idx = min(roots)
  is_src = segments[idx] == "src"
  check_mvn_idx = idx if is_src else -1
  if idx == 0 or is_src:
    # Check for a nested root directory.
    end_segments = segments[idx + 1:-1]
    src_segment = end_segments.index("src") if "src" in end_segments else -1
    if is_src:
      end_segments_idx = [
          end_segments.index(i)
          for i in ["java", "javatests", "src"]
          if i in end_segments
      ]
      if end_segments_idx:
        src_segment = min(end_segments_idx)
    if src_segment >= 0:
      next = end_segments[src_segment + 1]
      if next in ["com", "org", "net"]:
        # Check for common first element of java package, to avoid false
        # positives.
        idx += src_segment + 1
      elif next in ["main", "test"]:
        # Also accept maven style src/(main|test)/(java|resources).
        check_mvn_idx = idx + src_segment + 1
  # Check for (main|test)/(java|resources) after /src/.
  if check_mvn_idx >= 0 and check_mvn_idx < len(segments) - 2:
    if segments[check_mvn_idx + 1] in [
        "main", "test"
    ] and segments[check_mvn_idx + 2] in ["java", "resources"]:
      idx = check_mvn_idx + 2
  if idx < 0:
    return ".".join(segments)
  return ".".join(segments[idx + 1:])

def bazel_java_integration_test(
    name,
    srcs = [],
    deps = None,
    runtime_deps = [],
    data = [],
    jvm_flags = [],
    test_class = None,
    external_deps = [],
    # flag to allow bazel_integration_testing own tests to work
    add_bazel_data_dependency = True,
    versions = BAZEL_VERSIONS,
    tags = [],
    **kwargs):
  """A wrapper around java_test that create several java tests, one per version
     of Bazel.

     Args:
       versions: list of version of bazel to create a test for. Each test
         will be named `<name>/bazel<version>`.
       See java_test for the other arguments.
  """
  if not test_class:
    test_class = "%s.%s" % (_java_package(), name)
  add_deps = [
      str(Label("//java/build/bazel/tests/integration:workspace_driver")),
  ]
  if srcs:
    deps = (deps or []) + add_deps
  else:
    runtime_deps = runtime_deps + add_deps
  for version in versions:
    prop_rule = "%s/config%s" % (name, version)
    bazel_java_integration_test_properties_(
        name = prop_rule,
        bazel_version = version,
        external_deps = external_deps,
    )

    cur_data = data + external_deps + [prop_rule + ".properties"]
    if add_bazel_data_dependency:
      cur_data += ["@build_bazel_bazel_%s//:bazel_binary" % version.replace(".", "_")]
    native.java_test(
        name = "%s/bazel%s" % (name, version),
        jvm_flags = [
            "-Dbazel.configuration=$(location %s.properties)" % prop_rule
        ] + jvm_flags,
        srcs = srcs,
        data = cur_data,
        test_class = test_class,
        deps = deps,
        runtime_deps = runtime_deps,
        tags = tags,
        **kwargs)
  native.test_suite(
      name = name,
      tests = [":%s/bazel%s" % (name, version) for version in versions],
      tags = tags,
    )

def bazel_java_integration_test_deps(versions = BAZEL_VERSIONS):
  bazel_binaries(versions)

  jvm_maven_import_external(
      name = "com_google_guava",
      artifact = "com.google.guava:guava:jar:21.0",
      artifact_sha256 = "972139718abc8a4893fa78cba8cf7b2c903f35c97aaf44fa3031b0669948b480",
      server_urls = [
          "https://jcenter.bintray.com/",
          "https://repo1.maven.org/maven2",
      ],
  )

  jvm_maven_import_external(
      name = "org_hamcrest_core",
      artifact = "org.hamcrest:hamcrest-core:jar:1.3",
      artifact_sha256 = "66fdef91e9739348df7a096aa384a5685f4e875584cce89386a7a47251c4d8e9",
      server_urls = [
          "https://jcenter.bintray.com/",
          "https://repo1.maven.org/maven2",
      ],
  )

  jvm_maven_import_external(
      name = "org_junit",
      artifact = "junit:junit:jar:4.11",
      artifact_sha256 = "90a8e1603eeca48e7e879f3afbc9560715322985f39a274f6f6070b43f9d06fe",
      server_urls = [
          "https://jcenter.bintray.com/",
          "https://repo1.maven.org/maven2",
      ],
  )

  # buildeventstream protos
  # NOTE: this is a temporary location for the jar.  We could have built the proto
  # dependency from source (e.g., github.com/bazelbuild/bazel), but downloading the whole
  # of Bazel seems like overkill.
  java_import_external(
      name = "io_bazel_bep",
      licenses = ["notice"],  # Apache 2.0
      jar_sha256 = "9861e24a9d3a9d8ca1355b2c76e87450647a478cf9b5d547319c2d2d173388c0",
      jar_urls = [
          "https://github.com/bazelbuild/intellij/raw/983a509e55b78aaa5323adc9e2742db783d90d57/proto/proto_deps.jar",
      ],
  )

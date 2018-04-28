// Copyright 2018 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package build.bazel.tests.integration;

import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * This class interfaces with the Bazel repository cache.
 *
 * <p>Bazel can cache all the downloaded artifacts by their sha256 or sha-1 checksums, so that a
 * file with a given checksum is only downloaded once. This feature was experimental before 0.12.0
 * (excluded), and in later versions not experimental and enabled by default.
 *
 * <p>We can use this feature to provide our scratch workspace with pre-downloaded artifacts, so
 * that for an artifact with a checksum present in the cache, Bazel skips the downloading phase
 * entirely. This allows external dependencies to be consumed in the scratch workspace even if the
 * {@code block-network} tag is supplied to the test ({@code block-network} disables network
 * connectivity).
 *
 * <p>To do so, we reconstruct a cache prior to invoking Bazel with the files provided as {@code
 * external_deps} to the integration test and pass the location of the <i>de novo</i> cache to
 * Bazel.
 *
 * <p>The layout of the cache has not changed since it was introduced in Bazel: it is a directory
 * with descendants {@code content_addressable/<checksum type>/<checksum>/file}. Here, we only
 * support sha256 checksums ({@code <checksum type> = "sha256"}).
 *
 * <p>For this repository cache to prevent artifacts from being downloaded, the repository rules in
 * the scratch {@code WORKSPACE} must use the cache with sha256 checksums. This means that the
 * following rules won't benefit from this implementation and would still use the network: {@code
 * maven_jar}, {@code git_repository}, {@code new_git_repository}. Moreover, the sha256 checksum
 * must be specified (it is optional in most rules to make development easier).
 */
public class RepositoryCache {

  private final Path cachePath;

  // Repository cache subdirectories
  private static final String CAS_DIR = "content_addressable";

  // Bazel repository cache renames cached files to this value to simplify lookup.
  public static final String DEFAULT_CACHE_FILENAME = "file";

  // The only key type currently accepted here
  public static final String KEY_TYPE = "sha256";

  public RepositoryCache(Path cachePath) {
    this.cachePath = cachePath;
  }

  /** Add a file to the cache. The file name should be the sha256 digest of the file content. */
  public void put(Path sourcePath) throws IOException {
    Path sha256 = sourcePath.getFileName();
    // TODO: assert the sha256 key is valid, maybe even compare to content?
    Path cacheEntry = cachePath.resolve(CAS_DIR).resolve(KEY_TYPE).resolve(sha256);
    Files.createDirectories(cacheEntry);
    Files.createSymbolicLink(
        cacheEntry.resolve(DEFAULT_CACHE_FILENAME), sourcePath.toAbsolutePath());
  }

  /** Returns the options to pass to Bazel for it to use the repository cache. */
  public Collection<String> bazelOptions() {
    // After 0.12.0, repository_cache is no longer experimental
    // https://github.com/bazelbuild/bazel/commit/6e0933886d3c6b7f68075da4bdb08500ce2b6f86
    return Collections.singletonList("--experimental_repository_cache=" + cachePath);
  }

  /**
   * Make the cache read-only so that Bazel can't touch it, no dep can be added, if the file system
   * allows it.
   *
   * <p>On Mac OSX, for some reason, even with a {@code block-network} tag, the sandboxed Bazel can
   * still reach for new dependencies on Mac OSX, so we need an additional protection.
   *
   * <p>On Linux, freezing does not work, but anyway the sandboxed Bazel cannot reach for new
   * dependencies when {@code block-network} is enabled.
   */
  public void freeze() throws IOException {
    try {
      Files.walkFileTree(
          // We do not freeze sha-1, which is out of scope for this cache, only sha256.
          Files.createDirectories(cachePath.resolve(CAS_DIR).resolve(KEY_TYPE)),
          new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                throws IOException {
              if (file.getFileSystem().isReadOnly()) return FileVisitResult.CONTINUE;
              Set<PosixFilePermission> perms = new HashSet<>();
              perms.add(PosixFilePermission.OWNER_READ);
              Files.setPosixFilePermissions(file, perms);
              return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                throws IOException {
              Set<PosixFilePermission> perms = new HashSet<>();
              perms.add(PosixFilePermission.OWNER_READ);
              perms.add(PosixFilePermission.OWNER_EXECUTE); // For 'ls'
              Files.setPosixFilePermissions(dir, perms);
              return FileVisitResult.CONTINUE;
            }
          });
    } catch (FileSystemException ignored) {
      // Freezing does not work with the strong sandboxing on Linux.
    }
  }
}

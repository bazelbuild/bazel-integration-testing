package build.bazel.tests.integration;

import java.io.IOException;
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

/** This class interfaces with Bazel repository cache. */
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
   * Make the cache read-only so that Bazel can't touch it, no dep can be added. (For some reason,
   * even with a {@code block-network} tag, the sandboxed Bazel can still reach for new
   * dependencies.)
   */
  public void freeze() throws IOException {
    if (cachePath.getFileSystem().isReadOnly()) {
      return; // on a readonly filesystem we can't freeze
    }
    Files.walkFileTree(
        Files.createDirectories(cachePath),
        new SimpleFileVisitor<Path>() {
          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
              throws IOException {
            Set<PosixFilePermission> perms = new HashSet<>();
            perms.add(PosixFilePermission.OWNER_READ);
            Files.setPosixFilePermissions(file, perms);
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            Set<PosixFilePermission> perms = new HashSet<>();
            perms.add(PosixFilePermission.OWNER_READ);
            perms.add(PosixFilePermission.OWNER_EXECUTE); // For 'ls'
            Files.setPosixFilePermissions(dir, perms);
            return FileVisitResult.CONTINUE;
          }
        });
  }
}

#!/usr/bin/python2.7
#
# Copyright 2015 Google Inc. All Rights Reserved.
"""Regenerates readable JS and build logs."""

import os
import re
from subprocess import PIPE
from subprocess import Popen
import sys

# pylint: disable=global-variable-not-assigned
READABLE_TARGET_PATTERN = ("third_party/java_src/j2cl/transpiler/javatests/"
                           "com/google/j2cl/transpiler/readable/...:all")
INTEGRATION_TARGET_PATTERN = ("third_party/java_src/j2cl/transpiler/javatests/"
                              "com/google/j2cl/transpiler/integration/...:all")
JAVA_DIR = "third_party/java_src/j2cl/transpiler/javatests"
EXAMPLES_DIR = JAVA_DIR + "/com/google/j2cl/transpiler/readable/"
READABLE_TARGET_PATTERN = ("third_party/java_src/j2cl/transpiler/javatests/"
                           "com/google/j2cl/transpiler/readable/...")
JAVA8_BOOT_CLASS_PATH = ["--nocheck_visibility",
                         "--java_toolchain=//tools/jdk:toolchain_java8"]
SUCCESS_CODE = 0


class CmdExecutionError(Exception):
  """Indicates that a cmd execution returned a non-zero exit code."""


def extract_pattern(pattern_string, from_value):
  """Returns the regex matched value."""
  return re.compile(pattern_string).search(from_value).group(1)


def replace_pattern(pattern_string, replacement, in_value):
  """Returns the regex replaced value."""
  return re.compile(pattern_string).sub(replacement, in_value)


def run_cmd_get_output(cmd_args, include_stderr=False, cwd=None, shell=False):
  """Runs a cmd command and returns output as a string."""
  global SUCCESS_CODE

  process = (Popen(cmd_args,
                   shell=shell,
                   stdin=PIPE,
                   stdout=PIPE,
                   stderr=PIPE,
                   cwd=cwd))
  results = process.communicate()
  output = results[0]
  if include_stderr:
    output = (output + "\n" if output else "") + results[1]
  if process.wait() != SUCCESS_CODE:
    raise CmdExecutionError("cmd invocation " + str(cmd_args) + " failed with\n"
                            + results[1])

  return output


def get_js_binary_file_paths():
  """Finds and returns a list of js_binary bundle js file paths."""
  # Gather a list of the names of the test targets we care about
  test_targets = (run_cmd_get_output(
      ["blaze", "query", "filter('.*_binary', kind(%s, %s))" % (
          "js_binary", READABLE_TARGET_PATTERN)]).splitlines())
  test_targets = filter(bool, test_targets)

  return [
      size_target.replace("//", "blaze-bin/").replace(":", "/") + "-bundle.js"
      for size_target in test_targets
  ]


def get_readable_target_names():
  """Finds and returns the names of readable targets."""
  global READABLE_TARGET_PATTERN

  test_targets = (run_cmd_get_output(
      ["blaze", "query", "filter('.*:.*_j2cl_transpile', kind(%s, %s))" % (
          "j2cl_transpile", READABLE_TARGET_PATTERN)]).split("\n"))
  test_targets = filter(bool, test_targets)

  return [
      extract_pattern(".*readable/(.*?):.*_j2cl_transpile",
                      size_target) for size_target in test_targets
  ]


def blaze_build(target_names):
  """Blaze build everything in 1-go, for speed."""
  args = ["blaze", "build"]
  args += [EXAMPLES_DIR + target_name + ":" + target_name + "_j2cl_transpile"
           for target_name in target_names]
  args += JAVA8_BOOT_CLASS_PATH
  return run_cmd_get_output(args)


def replace_transpiled_js(target_names):
  """Copy and reformat and replace with Blaze built JS."""

  find_command_js_map_sources = ["find", EXAMPLES_DIR, "-name", "*.js.map"]

  # Remove old map files before unzipping the new ones
  run_cmd_get_output(find_command_js_map_sources + ["-exec", "rm", "{}", ";"])

  # Copy all the zips in one COPY command so that some of the
  # network communication is parallelized.
  if not os.path.isdir("/tmp/js.zip"):
    os.mkdir("/tmp/js.zip")
  run_cmd_get_output(
      ["cp -f blaze-bin/%s**/*.js.zip /tmp/js.zip" % EXAMPLES_DIR],
      shell=True)

  for target_name in target_names:
    zip_file_path = "/tmp/js.zip/%s_j2cl_transpile.js.zip" % target_name
    # Special case for readable example java.lang.String
    if "javalang" in target_name:
      extractDir = EXAMPLES_DIR + target_name
    else:
      extractDir = JAVA_DIR + "/"
    run_cmd_get_output(["unzip", "-o", "-d", extractDir, zip_file_path])

  # Ignore files under natives_sources/ since these are not generated.
  find_command_js_sources = ["find", EXAMPLES_DIR, "-name", "*.js", "-not",
                             "-path", "**/native_sources/*"]

  find_command_js_test_sources = ["find", EXAMPLES_DIR, "-name", "*.js.txt",
                                  "-not", "-path", "**/native_sources/*"]

  # Format .js files
  run_cmd_get_output(find_command_js_sources +
                     ["-exec", "/usr/bin/clang-format", "-i", "{}", "+"])

  # Remove the old .js.txt files (results from the last run)
  run_cmd_get_output(find_command_js_test_sources + ["-exec", "rm", "{}", ";"])

  # Move the newly unzipped .js => .js.txt
  run_cmd_get_output(find_command_js_sources + ["-exec", "mv", "{}", "{}.txt",
                                                ";"])


def gather_closure_warnings():
  """Gather Closure compiler warnings.

  Deletes just part of Blaze's cache so that it is forced to rebuild just the
  js_binary targets. The resulting build logs are split and saved.
  """
  global READABLE_TARGET_PATTERN

  # Delete the old build.log files before we regenerate them.
  find_command_build_logs = ["find", EXAMPLES_DIR, "-name", "build.log"]
  run_cmd_get_output(find_command_build_logs + ["-exec", "rm", "{}", ";"])

  run_cmd_get_output(["rm", "-fr"] + get_js_binary_file_paths())

  # Build both all readable and integrtion targets in one build command so that
  # Blaze build parallelize all the work. Saves a lot of time.
  args = ["blaze", "build", READABLE_TARGET_PATTERN, INTEGRATION_TARGET_PATTERN
         ] + JAVA8_BOOT_CLASS_PATH
  build_logs = run_cmd_get_output(args, include_stderr=True)
  build_logs = build_logs.split("____From Compiling JavaScript ")[1:]
  build_logs = filter(None, build_logs)
  for build_log in build_logs:
    # Remove unstable build timing lines.
    build_log = "\n".join([
        line
        for line in build_log.splitlines()
        if not line.startswith("_") and "  Compiling" not in line and "Running"
        not in line and "Building" not in line
    ])

    # Remove folder path spam.
    build_log = build_log.replace(
        "blaze-out/gcc-4.X.Y-crosstool-v18-hybrid-grtev4-k8-fastbuild/bin/", "")
    # Remove stable (but occasionally changing) line number details.
    build_log = replace_pattern(r"\:([0-9]*)\:", "", build_log)
    # Filter out the unstable ", ##% typed" message
    percent_typed_msg = (extract_pattern(r"g\(s\)(, .*? typed)", build_log))
    build_log = build_log.replace(percent_typed_msg, "")

    build_log_path = extract_pattern("//(.*?):", build_log) + "/build.log"
    # Don't write build.log files for integration tests.
    if "readable/" not in build_log_path:
      continue
    with open(build_log_path, "w") as build_log_file:
      build_log_file.write(build_log)


def main(argv=sys.argv):
  print "Generating readable JS and build logs:"
  readable_target_names = get_readable_target_names()

  print "  Blaze building everything"
  blaze_build(readable_target_names)

  print "  Copying and reformatting transpiled JS"
  replace_transpiled_js(readable_target_names)

  no_logs = (len(argv) > 1 and argv[1] == "--nologs")
  if no_logs:
    print "  Skipping logs!!!"
  else:
    print "  Re-Closure compiling examples to gather logs"
    gather_closure_warnings()

  print "run 'git gui' to see changes"


main()

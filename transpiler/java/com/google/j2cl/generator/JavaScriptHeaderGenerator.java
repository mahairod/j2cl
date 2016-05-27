/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.j2cl.generator;

import com.google.j2cl.ast.AnonymousJavaType;
import com.google.j2cl.ast.JavaType;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.errors.Errors;
import com.google.j2cl.generator.visitors.Import;
import com.google.j2cl.generator.visitors.ImportGatheringVisitor.ImportCategory;
import com.google.j2cl.generator.visitors.ImportUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * Generates JavaScript source header files.
 */
public class JavaScriptHeaderGenerator extends JavaScriptGenerator {

  public JavaScriptHeaderGenerator(
      Errors errors, boolean declareLegacyNamespace, JavaType javaType) {
    super(errors, declareLegacyNamespace, javaType);
  }

  @Override
  public String renderOutput() {
    TypeDescriptor selfTypeDescriptor = javaType.getDescriptor().getRawTypeDescriptor();
    Import selfImport = new Import(selfTypeDescriptor.getSimpleName(), selfTypeDescriptor);
    String binaryName = javaType.getDescriptor().getRawTypeDescriptor().getBinaryName();
    sourceBuilder.appendln("/**");
    sourceBuilder.appendln(" * @fileoverview Header transpiled from %s", binaryName);
    sourceBuilder.appendln(" *");
    sourceBuilder.appendln(" * @suppress {lateProvide}");
    sourceBuilder.appendln(" */");
    sourceBuilder.appendln("goog.module('%s');", selfImport.getHeaderModulePath());

    if (declareLegacyNamespace && javaType.getDescriptor().isJsType()
        && !(javaType instanceof AnonymousJavaType)) {
      // Even if opted into declareLegacyNamespace, this only makes sense for classes that are
      // intended to be accessed from the native JS. Thus we only emit declareLegacyNamespace
      // for non-anonymous JsType classes.
      sourceBuilder.appendln("goog.module.declareLegacyNamespace();");
    }

    sourceBuilder.newLine();
    sourceBuilder.newLine();
    sourceBuilder.appendln(
        "// Imports headers for both eager and lazy dependencies to ensure that");
    sourceBuilder.appendln("// all files are included in the dependency tree.");

    Set<String> requiredPaths = new HashSet<>();
    // goog.require(...) for eager imports.
    for (Import eagerImport : ImportUtils.sortedList(importsByCategory.get(ImportCategory.EAGER))) {
      String alias = eagerImport.getAlias();
      String path = eagerImport.getHeaderModulePath();
      if (requiredPaths.add(path)) {
        sourceBuilder.appendln("let _%s = goog.require('%s');", alias, path);
      }
    }
    // goog.require(...) for lazy imports.
    for (Import eagerImport : ImportUtils.sortedList(importsByCategory.get(ImportCategory.LAZY))) {
      String alias = eagerImport.getAlias();
      String path = eagerImport.getHeaderModulePath();
      if (requiredPaths.add(path)) {
        sourceBuilder.appendln("let _%s = goog.require('%s');", alias, path);
      }
    }
    // externs imports are always available in the browser and don't need a header reference.
    sourceBuilder.newLine();
    sourceBuilder.newLine();

    String className = environment.aliasForType(javaType.getDescriptor());
    String implementationPath = selfImport.getImplModulePath();
    sourceBuilder.appendln("// Re-exports the implementation.");
    sourceBuilder.appendln("var %s = goog.require('%s');", className, implementationPath);
    sourceBuilder.appendln("exports = %s;", className);
    return sourceBuilder.build();
  }

  @Override
  public String getSuffix() {
    return ".js";
  }
}

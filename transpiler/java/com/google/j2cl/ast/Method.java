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
package com.google.j2cl.ast;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.j2cl.ast.processors.Context;
import com.google.j2cl.ast.processors.Visitable;
import com.google.j2cl.ast.sourcemap.SourcePosition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;

/**
 * Method declaration.
 */
@Visitable
@Context
public class Method extends Node {
  @Visitable MethodDescriptor methodDescriptor;
  @Visitable List<Variable> parameters = new ArrayList<>();
  @Visitable Block body;
  private boolean isAbstract;
  private boolean isOverride;
  private String jsDocDescription;
  private boolean isFinal;
  private BitSet parameterOptionality;

  private Method(
      MethodDescriptor methodDescriptor,
      List<Variable> parameters,
      Block body,
      boolean isAbstract,
      boolean isOverride,
      boolean isFinal,
      String jsDocDescription) {
    this.methodDescriptor = checkNotNull(methodDescriptor);
    this.parameters.addAll(checkNotNull(parameters));
    this.isAbstract = isAbstract;
    this.isOverride = isOverride;
    this.isFinal = isFinal;
    this.jsDocDescription = jsDocDescription;
    this.body = checkNotNull(body);
  }

  public MethodDescriptor getDescriptor() {
    return methodDescriptor;
  }

  public List<Variable> getParameters() {
    return parameters;
  }

  public Block getBody() {
    return body;
  }

  public boolean isConstructor() {
    return methodDescriptor.isConstructor();
  }

  public boolean isNative() {
    return methodDescriptor.isNative();
  }

  public boolean isAbstract() {
    return this.isAbstract;
  }

  public void setAbstract(boolean isAbstract) {
    this.isAbstract = isAbstract;
  }

  public boolean isOverride() {
    return this.isOverride;
  }

  public void setOverride(boolean isOverride) {
    this.isOverride = isOverride;
  }

  public boolean isParameterOptional(int i) {
    return parameterOptionality.get(i);
  }

  public boolean isFinal() {
    return this.isFinal;
  }

  public void setFinal(boolean isFinal) {
    this.isFinal = isFinal;
  }

  public String getJsDocDescription() {
    return jsDocDescription;
  }

  public boolean isPrimaryConstructor() {
    if (isConstructor()) {
      if (getDescriptor().getEnclosingClassTypeDescriptor().isOrSubclassesJsConstructorClass()) {
        return !AstUtils.hasThisCall(this);
      }
    }
    return false;
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_Method.visit(processor, this);
  }

  /**
   * A Builder for easily and correctly creating modified versions of methods.
   *
   * <p>Takes care of the busy work of keeping parameter list and method descriptor parameter type
   * list in sync.
   */
  public static class Builder {

    private MethodDescriptor methodDescriptor;
    private List<Variable> parameters = new ArrayList<>();
    private List<Statement> statements = new ArrayList<>();
    private boolean isAbstract;
    private boolean isOverride;
    private String jsDocDescription;
    private boolean isFinal;
    private SourcePosition bodyJavaSourcePosition = SourcePosition.UNKNOWN;
    private BitSet parameterOptionality = new BitSet();

    public static Builder fromDefault() {
      return new Builder();
    }

    public static Builder from(Method method) {
      Builder builder = new Builder();
      builder.methodDescriptor = method.getDescriptor();
      builder.parameters = Lists.newArrayList(method.getParameters());
      builder.statements = Lists.newArrayList(method.getBody().getStatements());
      builder.isAbstract = method.isAbstract();
      builder.isOverride = method.isOverride();
      builder.jsDocDescription = method.getJsDocDescription();
      builder.isFinal = method.isFinal();
      builder.bodyJavaSourcePosition = method.getBody().getSourcePosition();
      builder.parameterOptionality = method.parameterOptionality;
      return builder;
    }

    public Builder addParameters(Variable... parameters) {
      return addParameters(Arrays.asList(parameters));
    }

    public Builder addParameters(Iterable<Variable> newParameters) {
      Iterables.addAll(parameters, newParameters);
      return this;
    }

    public Builder addStatements(Statement... statements) {
      Collections.addAll(this.statements, statements);
      return this;
    }

    public Builder addStatements(List<Statement> statements) {
      this.statements.addAll(statements);
      return this;
    }

    public Builder addStatement(int index, Statement statement) {
      this.statements.add(index, statement);
      return this;
    }

    public Builder setParameters(Variable... parameters) {
      this.parameters = Arrays.asList(parameters);
      return this;
    }

    public Builder setParameters(List<Variable> parameters) {
      this.parameters = new ArrayList<>(parameters);
      return this;
    }

    public Builder setMethodDescriptor(MethodDescriptor methodDescriptor) {
      this.methodDescriptor = methodDescriptor;
      return this;
    }

    public Builder clearStatements() {
      statements.clear();
      return this;
    }

    public Builder removeStatement(int index) {
      statements.remove(index);
      return this;
    }

    public Builder setEnclosingClass(TypeDescriptor enclosingClassTypeDescriptor) {
      this.methodDescriptor =
          MethodDescriptor.Builder.from(methodDescriptor)
              .setEnclosingClassTypeDescriptor(enclosingClassTypeDescriptor)
              .build();
      return this;
    }

    public Builder setIsOverride(boolean isOverride) {
      this.isOverride = isOverride;
      return this;
    }

    public Builder setIsFinal(boolean isFinal) {
      this.isFinal = isFinal;
      return this;
    }

    public Builder setIsAbstract(boolean isAbstract) {
      this.isAbstract = isAbstract;
      return this;
    }

    public Builder setJsDocDescription(String jsDocDescription) {
      this.jsDocDescription = jsDocDescription;
      return this;
    }

    public Builder setJsInfo(JsInfo jsInfo) {
      this.methodDescriptor =
          MethodDescriptor.Builder.from(methodDescriptor).setJsInfo(jsInfo).build();
      return this;
    }

    public Builder setParameterOptional(int parameterIndex, boolean isOptional) {
      parameterOptionality.set(parameterIndex, isOptional);
      return this;
    }

    public Method build() {
      Block body = new Block(statements);
      body.setSourcePosition(bodyJavaSourcePosition);
      Method method =
          new Method(
              // Update method descriptor parameter types from actual parameter types.
              MethodDescriptor.Builder.from(methodDescriptor)
                  .setParameterTypeDescriptors(
                      Iterables.transform(
                          parameters,
                          new Function<Variable, TypeDescriptor>() {
                            @Override
                            public TypeDescriptor apply(Variable parameter) {
                              return parameter.getTypeDescriptor();
                            }
                          }))
                  .build(),
              parameters,
              body,
              isAbstract,
              isOverride,
              isFinal,
              jsDocDescription);
      method.parameterOptionality = parameterOptionality;
      return method;
    }
  }
}

/*
 * Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package java.lang.invoke;

import java.io.Serializable;

/**
 * See
 * <a href="https://docs.oracle.com/javase/8/docs/api/java/lang/invoke/SerializedLambda.html">the
 * official Java API doc</a> for details.
 */
public final class SerializedLambda implements Serializable {

  public Object getCapturedArg(int i) {
    return null;
  }

  public String getFunctionalInterfaceClass() {
    return null;
  }

  public String getFunctionalInterfaceMethodName() {
    return null;
  }

  public String getFunctionalInterfaceMethodSignature() {
    return null;
  }

  public String getImplClass() {
    return null;
  }

  public int getImplMethodKind() {
    return 0;
  }

  public String getImplMethodName() {
    return null;
  }

  public String getImplMethodSignature() {
    return null;
  }
}

/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.j2cl.transpiler.integration.interfaceabstractimpl;

/**
 * Multiple inheritance.
 * A -> I, J, B
 * I -> W
 * J -> W, X, Z
 * B -> W, X, Y
 * Z -> X
 * All funIn**() functions are unimplemented in A, barIn**() functions are implemented in A's
 * super classes.
 */
interface W {
  int funInW();

  int barInW();
}

interface X {
  int funInX();

  int barInX();
}

interface Y {
  int funInY();

  int barInY();
}

interface Z extends X {
  int funInZ();

  int barInZ();

  int klmInZ();
}

interface I extends W {
  int funInI();

  int barInI();
}

interface J extends W, Z, X {
  int funInJ();

  int barInJ();
}

class E {
  public int klmInZ() {
    return 42;
  }
}

class D extends E {}

abstract class C extends D implements Z {
  @Override
  public int barInZ() {
    return 1;
  }

  @Override
  public int barInX() {
    return 2;
  }

  public abstract int barInI();

  // klmInZ from grandparent is accidentally overridden
}

abstract class B extends C implements W, X, Y {
  @Override
  public int barInW() {
    return 3;
  }

  @Override
  public abstract int barInY();
}

abstract class A extends B implements I, J {
  @Override
  public int barInI() {
    return 4;
  }

  // All funIn**() functions should be added otherwise JSCompiler errors.

  // barIn**() functions should be not added because they are inherited from super classes.
}

class SubA extends A {
  @Override
  public int funInI() {
    return 5;
  }

  @Override
  public int funInW() {
    return 6;
  }

  @Override
  public int funInJ() {
    return 7;
  }

  @Override
  public int barInJ() {
    return 8;
  }

  @Override
  public int funInZ() {
    return 9;
  }

  @Override
  public int funInX() {
    return 10;
  }

  @Override
  public int funInY() {
    return 11;
  }

  @Override
  public int barInY() {
    return 12;
  }
}

public class Main {
  public static void main(String... args) {
    SubA subA = new SubA();
    assert (subA.barInI() == 4);
    assert (subA.barInJ() == 8);
    assert (subA.barInW() == 3);
    assert (subA.barInX() == 2);
    assert (subA.barInY() == 12);
    assert (subA.barInZ() == 1);
    assert (subA.funInI() == 5);
    assert (subA.funInJ() == 7);
    assert (subA.funInW() == 6);
    assert (subA.funInX() == 10);
    assert (subA.funInY() == 11);
    assert (subA.funInZ() == 9);
    assert (subA.klmInZ() == 42);
  }
}

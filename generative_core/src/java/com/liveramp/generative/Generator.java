package com.liveramp.generative;

import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

public class Generator<T> implements Arbitrary<T> {

  private final Arbitrary<T> internal;
  private Generative2 gen;

  public Generator(Arbitrary<T> internal, Generative2 gen) {
    this.internal = internal;
    this.gen = gen;
  }

  @Override
  public Generator<T> gen(Generative2 gen) {
    return new Generator<>(internal, gen);
  }

  public T get() {
    return gen.generate(internal);
  }

  @Override
  @NotNull
  public T get(Random r) {
    return internal.get(r);
  }

  @Override
  public List<T> shrink(T val) {
    return internal.shrink(val);
  }

  @Override
  public Stream<T> stream(Random r) {
    return internal.stream(r);
  }

  @Override
  public <R> Generator<R> map(Function<T, R> fn) {
    return new Generator<>(internal.map(fn), gen);
  }

  @Override
  public <R> Generator<R> map(Function<T, R> fn, Function<R, T> reverse) {
    return new Generator<>(internal.map(fn, reverse), gen);
  }

  @Override
  public <R> Generator<R> flatMap(Function<T, Arbitrary<R>> fn, Function<R, T> reverse) {
    return new Generator<>(internal.flatMap(fn, reverse), gen);
  }

  @Override
  public <R> Generator<R> flatMap(Function<T, Arbitrary<R>> fn) {
    return new Generator<>(internal.flatMap(fn), gen);
  }
}

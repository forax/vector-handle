package com.github.forax.vectorhandle;

import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;

import static java.lang.invoke.MethodHandles.lookup;
import static org.junit.jupiter.api.Assertions.*;

public class VectorHandleTest {
  @Test
  public void applyII() {
    var vh = VectorHandle.of(lookup(), int.class, int.class);

    var dest = new int[3];
    var a = new int[] { 1, 2, 3 };
    vh.apply(dest, a, x -> - x);
    assertArrayEquals(new int[] { -1, -2, -3 }, dest);
  }

  @Test
  public void applyFFF() {
    var vh = VectorHandle.of(lookup(), float.class, float.class, float.class);

    var dest = new float[4];
    var a = new float[] { 1, 2, 3, 4 };
    var b = new float[] { 4, 4, 4, 4 };
    vh.apply(dest, a, b, (x, y) -> x + y * 2);
    assertArrayEquals(new float[] { 9, 10, 11, 12 }, dest);
  }
}
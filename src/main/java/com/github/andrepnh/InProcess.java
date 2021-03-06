package com.github.andrepnh;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

public class InProcess {

  @State(Scope.Benchmark)
  public static class LocalCallState {
    public byte[] preAllocatedArray = new byte[Payload.ACTUAL.length];
  }

  @Benchmark
  public byte[] localCall(LocalCallState payloadCopy) {
    return newPayload(payloadCopy.preAllocatedArray);
  }

  @CompilerControl(CompilerControl.Mode.DONT_INLINE)
  public byte[] newPayload(byte[] copy) {
    // Since most benchmarks end up reading from or writing to an array, we should do this here too
    System.arraycopy(Payload.ACTUAL, 0, copy, 0, copy.length);
    return copy;
  }
}

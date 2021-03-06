package com.github.andrepnh;

import io.undertow.Undertow;
import io.undertow.util.HeaderMap;
import java.io.IOException;
import java.nio.ByteBuffer;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

public class Http {
  @State(Scope.Thread)
  public static class LocalHttpState {
    private Undertow server;

    public OkHttpClient client;

    public Request request;

    @Setup(Level.Iteration)
    public void startServer() {
      server =
          Undertow.builder()
              .addHttpListener(8080, "localhost")
              .setHandler(
                  serverExchange -> {
                    var responseBody = ByteBuffer.wrap(Payload.copy());
                    HeaderMap responseHeaders = serverExchange.getResponseHeaders();
                    Payload.RESPONSE_HEADERS.forEach(
                        (name, values) -> responseHeaders.putAll(name, values));
                    serverExchange
                        .getRequestReceiver()
                        .receivePartialBytes(
                            (exchange, content, last) -> {
                              if (last) {
                                exchange.getResponseSender().send(responseBody);
                              }
                            });
                  })
              .build();
      server.start();
      client = new OkHttpClient();
      request = new Request.Builder().url("http://localhost:8080").build();
    }

    @TearDown(Level.Iteration)
    public void stopServer() {
      server.stop();
      Utils.close(client);
    }
  }

  @State(Scope.Thread)
  public static class RemoteHttpState {
    public OkHttpClient client;

    public Request request;

    @Setup(Level.Iteration)
    public void prepareRequest() {
      client = new OkHttpClient();
      request =
          new Request.Builder()
              .url("http://en.wikipedia.org/static/images/project-logos/enwiki.png")
              .build();
    }

    @TearDown(Level.Iteration)
    public void closeClient() {
      Utils.close(client);
    }
  }

  @State(Scope.Thread)
  public static class RemoteHttpsState {
    public OkHttpClient client;

    public Request request;

    @Setup(Level.Iteration)
    public void prepareRequest() {
      client = new OkHttpClient();
      request =
          new Request.Builder()
              .url("https://en.wikipedia.org/static/images/project-logos/enwiki.png")
              .build();
    }

    @TearDown(Level.Iteration)
    public void closeClient() {
      Utils.close(client);
    }
  }

  @Benchmark
  public String localHttpRequest(LocalHttpState state) throws IOException {
    try (var response = state.client.newCall(state.request).execute();
        var body = response.body()) {
      return body.string();
    }
  }

  @Benchmark
  public String remoteHttpRequest(RemoteHttpState state) throws IOException {
    try (var response = state.client.newCall(state.request).execute();
        var body = response.body()) {
      return body.string();
    }
  }

  @Benchmark
  public String remoteHttpsRequest(RemoteHttpsState state) throws IOException {
    try (var response = state.client.newCall(state.request).execute();
        var body = response.body()) {
      return body.string();
    }
  }
}

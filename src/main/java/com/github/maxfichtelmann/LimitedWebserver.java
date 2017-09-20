package com.github.maxfichtelmann;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Hello world!
 *
 */
public class LimitedWebserver {

	private final ExecutorService threadpool;
	private final long waitTime;

	public LimitedWebserver(ExecutorService threadpool, long waitTime) {
		this.threadpool = threadpool;
		this.waitTime = waitTime;
	}

	public void dispatch(Socket socket, String payload) {
		threadpool.submit(() -> {
			try (OutputStream out = socket.getOutputStream(); InputStream in = socket.getInputStream()) {

				BufferedReader reader = new BufferedReader(new InputStreamReader(in));
				reader.lines().filter(String::isEmpty).findAny();
				System.out.println("read complete");
				
				TimeUnit.MILLISECONDS.sleep(waitTime);
				Writer writer = new OutputStreamWriter(out, StandardCharsets.US_ASCII);
				writer.write("HTTP/1.0 200 OK\r\n");
				writer.write("Server: limited-web-server\r\n");
				writer.write("Content-Type: text/plain\r\n");
				writer.write("Content-Length: " + payload.getBytes(StandardCharsets.US_ASCII).length + "\r\n");
				writer.write("\r\n");
				writer.write(payload);
				writer.flush();
				writer.close();
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		});
	}

	public static void main(String[] args) throws IOException {
		int port = Integer.getInteger("PORT", 8000);
		int maxParallel = Integer.getInteger("MAX_PARALLEL", 5);
		long duration = Long.getLong("DURATION", 1000);

		System.out.printf("started [port=%d, maxParallel=%d, duration=%dms]%n", port, maxParallel, duration);

		AtomicLong requestCounter = new AtomicLong(0);

		ExecutorService threadPool = Executors.newFixedThreadPool(maxParallel);
		LimitedWebserver server = new LimitedWebserver(threadPool, duration);
		try (ServerSocket socket = new ServerSocket(port)) {
			while (true) {
				server.dispatch(socket.accept(), "done: " + requestCounter.incrementAndGet() + "\n");
			}
		}
	}
}

/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.api;

import com.google.inject.Inject;
import com.google.inject.Injector;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import tools.refinery.language.web.api.dto.ProblemInput;
import tools.refinery.language.web.api.dto.RefineryResponse;
import tools.refinery.language.web.api.dto.SemanticsRequest;
import tools.refinery.language.web.api.sink.ResponseSink;
import tools.refinery.language.web.tests.AwaitTerminationExecutorServiceProvider;
import tools.refinery.language.web.tests.DelegatingExecutorServiceProvider;
import tools.refinery.language.web.tests.ProblemWebInjectorProvider;
import tools.refinery.language.web.xtext.server.ThreadPoolExecutorServiceProvider;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(InjectionExtension.class)
@InjectWith(ProblemWebInjectorProvider.class)
class SemanticsWorkerTest {
	@Inject
	private Injector injector;

	@Inject
	private DelegatingExecutorServiceProvider executorServices;

	private AwaitTerminationExecutorServiceProvider awaitTerminationExecutorServiceProvider;

	private final SingleThreadExecutorServiceProvider workerExecutorServices =
			new SingleThreadExecutorServiceProvider();

	private ExecutorService semanticsExecutor;

	private final CountDownLatch release = new CountDownLatch(1);

	@BeforeEach
	void beforeEach() {
		awaitTerminationExecutorServiceProvider = new AwaitTerminationExecutorServiceProvider();
		executorServices.setDelegate(awaitTerminationExecutorServiceProvider);
		semanticsExecutor = workerExecutorServices.get(ThreadPoolExecutorServiceProvider.SEMANTICS_EXECUTOR);
	}

	@AfterEach
	void afterEach() {
		// Let the requests run even if the test failed before releasing them, and wait for them to finish before
		// the injector's global state is restored. The requests finish first, so that each cancels its own timeout
		// before the scheduler of timeouts shuts down. Every step runs, even if this thread is interrupted, and only
		// then does the teardown fail.
		release.countDown();
		var timeoutExecutor = workerExecutorServices.getScheduled(
				ThreadPoolExecutorServiceProvider.MODEL_GENERATION_TIMEOUT_EXECUTOR);
		boolean requestsDone = shutdownAndAwait(semanticsExecutor, Duration.ofMinutes(5));
		boolean timeoutsDone;
		try {
			workerExecutorServices.dispose();
		} finally {
			timeoutsDone = shutdownAndAwait(timeoutExecutor, Duration.ofMinutes(1));
			// The provider gives up waiting at once on an interrupted thread: wait first, then leave it interrupted.
			boolean interrupted = Thread.interrupted();
			try {
				awaitTerminationExecutorServiceProvider.dispose();
			} finally {
				if (interrupted) {
					Thread.currentThread().interrupt();
				}
			}
		}
		assertTrue(requestsDone, "Requests still running");
		assertTrue(timeoutsDone, "Timeouts still scheduled");
	}

	@Test
	void requestIsNotCancelledByAnotherCancelledRequest() throws Exception {
		// Hold the only semantics thread, so that both requests are scheduled before either runs: the live request
		// is initialized first and runs first, and the cancelled one is initialized while the live one waits.
		semanticsExecutor.submit(() -> {
			release.await();
			return null;
		});
		var live = new RecordingSink(false);
		var cancelled = new RecordingSink(true);
		createWorker().schedule(request(), live);
		createWorker().schedule(request(), cancelled);
		release.countDown();

		assertThat(live.await(), instanceOf(RefineryResponse.Success.class));
		assertThat(cancelled.await(), instanceOf(RefineryResponse.Cancelled.class));
	}

	@Test
	void timeoutsDoNotOutliveTheTest() throws InterruptedException {
		var executorServices = new SingleThreadExecutorServiceProvider();
		var timeoutExecutor = executorServices.getScheduled(
				ThreadPoolExecutorServiceProvider.MODEL_GENERATION_TIMEOUT_EXECUTOR);
		try {
			var timeout = timeoutExecutor.schedule(() -> {
			}, 10, TimeUnit.MINUTES);
			executorServices.dispose();
			// A request that ends after the scheduler shut down cancels its timeout only then.
			timeout.cancel(true);
			assertTrue(timeoutExecutor.awaitTermination(5, TimeUnit.SECONDS), "Timeout still scheduled");
		} finally {
			// Should the test fail, the timeout must not keep its thread waiting either.
			shutdownAndAwait(timeoutExecutor, Duration.ofSeconds(5));
		}
	}

	/**
	 * Shuts {@code executor} down and waits for its tasks to end, at most {@code timeout}, and as long again after
	 * interrupting them. It waits even if this thread is interrupted, and leaves it interrupted then.
	 *
	 * @return Whether the tasks ended.
	 */
	private static boolean shutdownAndAwait(ExecutorService executor, Duration timeout) {
		boolean interrupted = false;
		executor.shutdown();
		try {
			try {
				if (executor.awaitTermination(timeout.toNanos(), TimeUnit.NANOSECONDS)) {
					return true;
				}
			} catch (InterruptedException e) {
				interrupted = true;
			}
			executor.shutdownNow();
			long deadline = System.nanoTime() + timeout.toNanos();
			while (true) {
				try {
					return executor.awaitTermination(deadline - System.nanoTime(), TimeUnit.NANOSECONDS);
				} catch (InterruptedException e) {
					interrupted = true;
				}
			}
		} finally {
			if (interrupted) {
				Thread.currentThread().interrupt();
			}
		}
	}

	private SemanticsWorker createWorker() {
		var worker = new SemanticsWorker() {
			@Override
			protected Duration getTimeout() {
				// Only cancellation is under test: a slow machine must not time the request out.
				return Duration.ofMinutes(10);
			}
		};
		injector.injectMembers(worker);
		worker.setExecutorServiceProvider(workerExecutorServices);
		return worker;
	}

	private static SemanticsRequest request() {
		var input = new ProblemInput();
		input.setSource("""
				class Person.
				Person(alice).
				""");
		var request = new SemanticsRequest();
		request.setInput(input);
		return request;
	}

	/**
	 * Executors of one thread each, whatever the environment sets the server's thread counts to, so that requests
	 * run in the order they are scheduled.
	 */
	private static class SingleThreadExecutorServiceProvider extends ThreadPoolExecutorServiceProvider {
		@Override
		protected ExecutorService createInstance(String key) {
			return Executors.newSingleThreadExecutor();
		}

		@Override
		protected ScheduledExecutorService createScheduledInstance(String key) {
			// Drop the timeouts still scheduled when shut down, even those of requests that end afterwards, so that
			// no thread waits for their deadlines after the test.
			var executor = new ScheduledThreadPoolExecutor(1);
			executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
			executor.setRemoveOnCancelPolicy(true);
			return executor;
		}
	}

	private static class RecordingSink implements ResponseSink {
		private final boolean cancelled;
		private final CountDownLatch done = new CountDownLatch(1);
		private volatile RefineryResponse response;

		RecordingSink(boolean cancelled) {
			this.cancelled = cancelled;
		}

		@Override
		public void setResponse(int statusCode, RefineryResponse response) {
			this.response = response;
			done.countDown();
		}

		@Override
		public void updateStatus(Object status) {
			// This sink only records the response.
		}

		@Override
		public boolean isCancelled() {
			return cancelled;
		}

		@Override
		public boolean hasStatusReporting() {
			return false;
		}

		RefineryResponse await() throws InterruptedException {
			assertTrue(done.await(5, TimeUnit.MINUTES), "No response");
			return response;
		}
	}
}

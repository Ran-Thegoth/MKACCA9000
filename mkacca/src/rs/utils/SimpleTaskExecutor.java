package rs.utils;

import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Вспомогательный класс для реализации параллельного исполнения
 * 
 * @author Nick
 * 
 */
public class SimpleTaskExecutor {
	private static class SimpleTaskThreadFactory implements ThreadFactory {
		private static SimpleTaskThreadFactory SHARED_INSTANCE = new SimpleTaskThreadFactory();
		private ThreadGroup _group = new ThreadGroup("SimpleTask");
		private AtomicInteger _id = new AtomicInteger(1);

		private SimpleTaskThreadFactory() {

		}

		@Override
		public Thread newThread(Runnable r) {
			Thread result = new Thread(_group, r);
			result.setPriority(Thread.NORM_PRIORITY);
			result.setName("SimpleTaskThread" + _id.getAndIncrement());
			return result;
		}

	}

	private static class SimpleTaskExecutionHandler implements
			RejectedExecutionHandler {
		private static SimpleTaskExecutionHandler SHARED_INSTANCE = new SimpleTaskExecutionHandler();

		@Override
		public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
			if (executor.isShutdown())
				return;
			new Thread(r).start();
		}

	}

	private ScheduledThreadPoolExecutor _taskScheduler = new ScheduledThreadPoolExecutor(
			5, SimpleTaskThreadFactory.SHARED_INSTANCE,
			SimpleTaskExecutionHandler.SHARED_INSTANCE);
	private ThreadPoolExecutor _taskExecutor = new ThreadPoolExecutor(1, 5, 1l,
			TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(20));
	private Runnable SHUTDOWN_TASK = new Runnable() {
		@Override
		public void run() {
			_taskScheduler.shutdown();
			_taskExecutor.shutdown();

		}
	};

	private static SimpleTaskExecutor _instance = new SimpleTaskExecutor();

	public static SimpleTaskExecutor getInstance() {
		return _instance;
	}

	private SimpleTaskExecutor() {
		Runtime.getRuntime().addShutdownHook(new Thread(SHUTDOWN_TASK));
	}

	/**
	 * Выполнить r в отдельном потоке
	 * 
	 * @param r
	 */
	public void execute(Runnable r) {
		try {
			_taskExecutor.execute(r);
		} catch (RejectedExecutionException rje) {
			new Thread(r).start();
		}
	}

	/**
	 * Выполнять task с указанным периодом
	 * 
	 * @param task
	 *            - что исполнять
	 * @param interval
	 *            - период в милисекундах
	 * @return
	 */
	public Future<?> schedule(Runnable task, long interval) {
		return _taskScheduler.scheduleAtFixedRate(task, interval, interval,
				TimeUnit.MILLISECONDS);
	}

	/**
	 * Исполнить task через указанный интервал
	 * 
	 * @param task
	 *            - что исполнять
	 * @param interval
	 *            - интервал в милисекундах
	 * @return
	 */
	public Future<?> scheduleOnce(Runnable task, long interval) {
		return _taskScheduler.schedule(task, interval, TimeUnit.MILLISECONDS);
	}

	public void shutdown() {
		_taskExecutor.purge();
		_taskExecutor.shutdownNow();
		_taskScheduler.purge();
		_taskScheduler.shutdownNow();
	}
}

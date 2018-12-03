(ns sentry.executor
  (:import [java.util.concurrent Executors ArrayBlockingQueue ExecutorService LinkedBlockingQueue
                                 RejectedExecutionHandler ThreadPoolExecutor TimeUnit ThreadFactory]
           (com.google.common.util.concurrent MoreExecutors)))

(defn ^ExecutorService create-thread-pool
  ([worker-count ^ThreadFactory thread-factory]
   (ThreadPoolExecutor. (int worker-count)
                        (int worker-count)
                        0
                        TimeUnit/MILLISECONDS
                        (LinkedBlockingQueue.)
                        thread-factory))
  ([worker-count queue-size ^ThreadFactory thread-factory ^RejectedExecutionHandler rejection-policy]
   (ThreadPoolExecutor. (int worker-count)
                        (int worker-count)
                        0
                        TimeUnit/MILLISECONDS
                        (ArrayBlockingQueue. (int queue-size))
                        thread-factory
                        rejection-policy)))

(defn ^ExecutorService create-sync-executor []
  (MoreExecutors/newDirectExecutorService))

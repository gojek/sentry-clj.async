(ns sentry.executor-test
  (:require [clojure.test :refer :all]
            [sentry.executor :as executor])
  (:import (com.google.common.util.concurrent ListeningExecutorService ThreadFactoryBuilder)
           (java.util.concurrent ThreadPoolExecutor LinkedBlockingQueue RejectedExecutionHandler ThreadPoolExecutor$DiscardPolicy TimeUnit ArrayBlockingQueue)))

(deftest create-sync-executor-test
  (testing "it returns a listeningExecutorService type object"
    (is (instance? ListeningExecutorService (executor/create-sync-executor))))
  )

(deftest create-thread-pool-test
  (testing "creates ThreadPoolExecutor when worker count and thread factory are passed"
    (let [worker-count 10
          thread-factory (.build (ThreadFactoryBuilder.))
          thread-pool-executor (executor/create-thread-pool worker-count thread-factory)]
      (is (instance? ThreadPoolExecutor thread-pool-executor))
      (is (= worker-count (.getCorePoolSize thread-pool-executor)))
      (is (= worker-count (.getMaximumPoolSize thread-pool-executor)))
      (is (= 0 (.getKeepAliveTime thread-pool-executor TimeUnit/MILLISECONDS)))
      (is (instance? LinkedBlockingQueue (.getQueue thread-pool-executor)))
      (is (= thread-factory (.getThreadFactory thread-pool-executor)))))

  (testing "creates ThreadPoolExecutor when worker count, queue size, rejection policy and thread factory are passed"
    (let [worker-count 10
          queue-size 1
          thread-factory (.build (ThreadFactoryBuilder.))
          discard-policy (ThreadPoolExecutor$DiscardPolicy.)
          thread-pool-executor (executor/create-thread-pool worker-count queue-size thread-factory discard-policy)]
      (is (instance? ThreadPoolExecutor thread-pool-executor))
      (is (= worker-count (.getCorePoolSize thread-pool-executor)))
      (is (= worker-count (.getMaximumPoolSize thread-pool-executor)))
      (is (= 0 (.getKeepAliveTime thread-pool-executor TimeUnit/MILLISECONDS)))
      (is (instance? ArrayBlockingQueue (.getQueue thread-pool-executor)))
      (is (= thread-factory (.getThreadFactory thread-pool-executor)))
      (is (= discard-policy (.getRejectedExecutionHandler thread-pool-executor))))))



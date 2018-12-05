(ns sentry-clj.async-test
  (:require [clojure.test :refer :all]
            [raven-clj.core :as raven-clj]
            [sentry-clj.async :as sentry]
            [raven-clj.interfaces :as interfaces])
  (:import (java.util.concurrent ThreadFactory ThreadPoolExecutor TimeUnit ArrayBlockingQueue)
           (com.google.common.util.concurrent ListeningExecutorService)))

(deftest create-reporter-test
  (testing "returns config and does not start workers if not enabled"
    (let [config {:sync? true, :enabled false, :dsn "dsn", :env "env", :app-name "app_name", :worker-count 5, :queue-size 5, :thread-termination-wait-s 5}
          reporter (sentry/create-reporter config)]
      (is (= config (:config reporter)))
      (is (nil? (:workers reporter)))))

  (testing "returns config and listeningExecutorService worker if sync"
    (let [config {:sync? true, :enabled true, :dsn "dsn", :env "env", :app-name "app_name", :worker-count 5, :queue-size 5, :thread-termination-wait-s 5}
          reporter (sentry/create-reporter config)]
      (is (= config (:config reporter)))
      (is (instance? ListeningExecutorService (:workers reporter)))))

  (testing "returns config and ThreadPoolExecutor worker if not sync"
    (let [worker-count 5
          expected-config {:sync? false, :enabled true, :dsn "dsn", :env "env", :app-name "app_name", :worker-count worker-count, :queue-size 5, :thread-termination-wait-s 5}
          {:keys [config workers]} (sentry/create-reporter expected-config)
          thread-factory (.getThreadFactory workers)]
      (is (= expected-config config))
      (is (instance? ThreadPoolExecutor workers))
      (is (= worker-count (.getCorePoolSize workers)))
      (is (= worker-count (.getMaximumPoolSize workers)))
      (is (= 0 (.getKeepAliveTime workers TimeUnit/MILLISECONDS)))
      (is (instance? ArrayBlockingQueue (.getQueue workers)))
      (is (instance? ThreadFactory thread-factory)))))

(deftest sentry-report-test
  (testing "it calls sentry/capture with the correct parameters"
    (let [config {:enabled      true
                  :dsn          "sentry-dsn"
                  :sync?        false
                  :worker-count 10
                  :queue-size   1
                  :env          "production"
                  :app-name     "app-name"}
          stacktrace-args (atom nil)
          capture-args (promise)
          worker-conf (sentry/create-reporter config)
          level :warn
          expected-error (Throwable.)
          msgs "error!"
          stacktrace {:key "value"}
          expected-event {:message "error!", :level "warn", :environment "production"}]
      (with-redefs [interfaces/stacktrace (fn [event error app-name]
                                            (reset! stacktrace-args {:event event :error error :app-name app-name})
                                            stacktrace)
                    raven-clj/capture (fn [dsn interfaces-stacktrace]
                                        (deliver capture-args {:dsn dsn :stacktrace interfaces-stacktrace}))]
        (let [_ (sentry/sentry-report worker-conf level expected-error msgs)]
          (is (= @stacktrace-args {:event expected-event :error expected-error :app-name [(:app-name config)]}))
          (is (= (:event @stacktrace-args) expected-event))
          (is (= (:error @stacktrace-args) expected-error))
          (is (= (:app-name @stacktrace-args) [(:app-name config)])))))))

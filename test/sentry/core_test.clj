(ns sentry.core-test
  (:require [clojure.test :refer :all]
            [raven-clj.core :as raven-clj]
            [sentry.executor :as executor]
            [sentry.core :as sentry]
            [raven-clj.interfaces :as interfaces]
            [clojure.tools.logging :as log])
  (:import (java.util.concurrent ThreadFactory ThreadPoolExecutor$DiscardPolicy)))

(deftest create-reporter-test
  (testing "does not start workers if not enabled"
    (let [sync-executor-called? (atom false)
          config                {:enabled false
                                 :sync?   true}]
      (with-redefs [executor/create-sync-executor (fn [] (reset! sync-executor-called? true))]
        (sentry/create-reporter config)
        (is (not @sync-executor-called?)))))

  (testing "calls create-sync-executor if sync"
    (let [sync-executor-called? (atom false)
          config                {:enabled true
                                 :sync?   true}]
      (with-redefs [executor/create-sync-executor (fn [] (reset! sync-executor-called? true))]
        (sentry/create-reporter config)
        (is @sync-executor-called?))))

  (testing "calls create-thread-pool if not sync"
    (let [create-thread-pool-args (atom nil)
          config-worker-count     10
          config-queue-size       5
          config                  {:enabled      true
                                   :sync?        false
                                   :worker-count config-worker-count
                                   :queue-size   config-queue-size}]
      (with-redefs [executor/create-thread-pool (fn [worker-count queue-size thread-factory discard-policy]
                                                  (reset! create-thread-pool-args {:worker-count   worker-count
                                                                                   :queue-size     queue-size
                                                                                   :thread-factory thread-factory
                                                                                   :discard-policy discard-policy}))]
        (sentry/create-reporter config)
        (is (= (:worker-count @create-thread-pool-args) config-worker-count))
        (is (= (:queue-size @create-thread-pool-args) config-queue-size))
        (is (instance? ThreadFactory (:thread-factory @create-thread-pool-args)))
        (is (instance? ThreadPoolExecutor$DiscardPolicy (:discard-policy @create-thread-pool-args)))))))

(deftest sentry-report-test
  (testing "it calls sentry/capture with the correct parameters"
    (let [config           {:enabled      true
                            :dsn          "sentry-dsn"
                            :sync?        false
                            :worker-count 10
                            :queue-size   1
                            :env          "production"
                            :app-name     "app-name"}
          stacktrace-args  (atom nil)
          capture-args     (promise)
          worker-conf      (sentry/create-reporter config)
          level            :warn
          expected-error   (Throwable.)
          msgs             "error!"
          stacktrace       {:key "value"}
          expected-event   {:message "error!", :level "warn", :environment "production"}]
      (with-redefs [interfaces/stacktrace (fn [event error app-name]
                                            (reset! stacktrace-args {:event event :error error :app-name app-name})
                                            stacktrace)
                    raven-clj/capture     (fn [dsn interfaces-stacktrace]
                                            (deliver capture-args {:dsn dsn :stacktrace interfaces-stacktrace}))]
        (let [enqueued-task (sentry/sentry-report worker-conf level expected-error msgs)]
          (is (= @stacktrace-args {:event expected-event :error expected-error :app-name [(:app-name config)]}))
          (is (= (:event @stacktrace-args) expected-event))
          (is (= (:error @stacktrace-args) expected-error))
          (is (= (:app-name @stacktrace-args) [(:app-name config)])))))))

(ns sentry-clj.async
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log]
            [raven-clj.core :as sentry]
            [raven-clj.interfaces :as interfaces])
  (:import (java.util.concurrent ThreadFactory ExecutorService ThreadPoolExecutor$DiscardPolicy TimeUnit
                                 ArrayBlockingQueue ThreadPoolExecutor RejectedExecutionHandler)
           (com.google.common.util.concurrent ThreadFactoryBuilder MoreExecutors)
           (clojure.lang ExceptionInfo)))

(declare report-error)

(def ^:private sentry-exception-handler
  (reify Thread$UncaughtExceptionHandler
    (uncaughtException [_ _ ex]
      (report-error ex "Uncaught error in Sentry worker"))))

(defn- ^ExecutorService create-thread-pool
  ([worker-count queue-size ^ThreadFactory thread-factory ^RejectedExecutionHandler rejection-policy]
   (ThreadPoolExecutor. (int worker-count)
                        (int worker-count)
                        0
                        TimeUnit/MILLISECONDS
                        (ArrayBlockingQueue. (int queue-size))
                        thread-factory
                        rejection-policy)))

(defn- ^ExecutorService create-sync-executor []
  (MoreExecutors/newDirectExecutorService))

(defn- ^ThreadFactory thread-factory [name-format]
  (-> (ThreadFactoryBuilder.)
      (.setDaemon true)
      (.setNameFormat name-format)
      (.setUncaughtExceptionHandler sentry-exception-handler)
      (.build)))

(defn- ^ExecutorService start-workers [{:keys [sync? worker-count queue-size]}]
  (log/info "Creating Sentry workers")
  (if sync?
    (create-sync-executor)
    (create-thread-pool worker-count queue-size
                        (thread-factory "sentry-worker-%d")
                        (ThreadPoolExecutor$DiscardPolicy.))))
(defn sentry-report
  [{:keys [workers config]} level ^Throwable error & msgs]
  (let [{:keys [enabled dsn env app-name]} config]
    (when enabled
      (let [message (string/join " " (if (instance? ExceptionInfo error)
                                       (concat msgs ["\nData:" (ex-data error)])
                                       msgs))
            event {:message     message
                   :level       (name level)
                   :environment (name env)}
            ^Runnable task #(try
                              (sentry/capture dsn
                                              (interfaces/stacktrace event error [app-name]))
                              (catch Exception e
                                (log/warn e "Error while reporting error to sentry")))]
        (.submit ^ExecutorService workers task)))))

(defmacro logp [level ^Throwable error & msgs]
  `(if (instance? ExceptionInfo ~error)
     (log/logp ~level ~error ~@msgs (str "\nData: " (ex-data ~error)))
     (log/logp ~level ~error ~@msgs)))

(defmacro report-error
  [sentry-reporter ^Throwable error & msgs]
  `(do (logp :error ~error ~@msgs)
       (sentry-report ~sentry-reporter :error ~error ~@msgs)))

(defmacro report-warn
  [sentry-reporter ^Throwable error & msgs]
  `(do (logp :warn ~error ~@msgs)
       (sentry-report ~sentry-reporter :warning ~error ~@msgs)))

(defn create-reporter [{:keys [enabled dsn env app-name sync? worker-count queue-size thread-termination-wait-s]}]
  (let [enabled (if (nil? enabled) true enabled)
        config {:sync?                     (or sync? false)
                :enabled                   enabled
                :dsn                       dsn
                :env                       env
                :app-name                  app-name
                :worker-count              (or worker-count 10)
                :queue-size                (or queue-size 10)
                :thread-termination-wait-s (or thread-termination-wait-s 10)}]
    {:config  config
     :workers (when enabled
                (start-workers (select-keys config [:sync? :worker-count :queue-size])))}))

(defn shutdown-reporter
  [{:keys [config workers]}]
  (when workers
    (.shutdown ^ExecutorService workers)
    (.awaitTermination ^ExecutorService workers (:thread-termination-wait-s config) TimeUnit/SECONDS)
    (log/info "Stopped Sentry workers")))

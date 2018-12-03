# Sentry

A Clojure library wrapper around sentry which gives abilty to send message in sync or async

## Usage

Add it to `project.clj`: `[farm.gojek/sentry-clj.async "0.2.2"]`

#### Initialization

`(sentry-clj.async.core/create-reporter config)`
will create a Sentry reporter. You should save this in an atom or a `mount` state.

Where `config` is a map defined below with default configs

```
{:sync?                            false
 :enabled                          true 
 :dsn                              "dummy"    ;; to be populated
 :env                              "env"      ;; to be populated
 :app-name                         "app-name" ;; to be populated
 :worker-count                     10
 :queue-size                       10
 :thread-termination-wait-s        10}
```

#### Reporting errors

It exposes two macros:

`(report-error sentry-reporter exception "message")` for reporting the error

`(report-warn sentry-reporter exception "message")` for reporting the warning

#### Shutting down
`(sentry.core/shutdown-reporter sentry-reporter)`


## Example:

```clojure
(ns example
  (:require [sentry-clj.async.core :as sentry]))

(defonce reporter (atom nil))

;; Create a reporter and save it in a atom
(reset! reporter (sentry/create-reporter {:sync?                            false
                                          :enabled                          true
                                          :dsn                              "http://foo@gojek-sentry.golabs.io"
                                          :env                              :production
                                          :app-name                         "foo"
                                          ;; Number of worker threads.
                                          :worker-count                     10
                                          :queue-size                       10
                                          ;; Seconds to wait for the worker threads to terminate.
                                          :thread-termination-wait-s        10}))

;; Report an error.
(try
  (do
    (println "foo")
    (throw (ex-info "Alas! Something is amiss!" {})))
  (catch Throwable e
    (sentry/report-error @reporter e "Something is terribly wrong!")))
    
;; Shut down the reporter.
(sentry/shutdown-reporter @reporter)
(reset! reporter nil)
```

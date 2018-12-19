# Sentry-clj.async

<p align="center">
  <a href="https://travis-ci.org/gojektech/sentry-clj.async">
    <img src="https://travis-ci.org/gojektech/sentry-clj.async.svg?branch=master" alt="Build Status" />
  </a>
  <a href='https://coveralls.io/github/gojektech/sentry-clj.async'>
    <img src='https://coveralls.io/repos/github/gojektech/sentry-clj.async/badge.svg' alt='Coverage Status' />
  </a>
  <a href='https://clojars.org/tech.gojek/sentry-clj.async'>
    <img src='https://img.shields.io/clojars/v/tech.gojek/sentry-clj.async.svg' alt='Clojars Project' />
  </a>
</p>

* [Description](#description)
* [Example](#example)
* [License](#license)

## Description

A Clojure library wrapper around [sentry](https://docs.sentry.io/) which gives abilty to send message in sync or async

#### Initialization

Sentry reporter can be created using the function:

`(sentry-clj.async/create-reporter config)`

This should be stored in an atom or `mount` state, as this will be passed to the report macros.

`config` is a map defined below with default values

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

Events could be reported to sentry using one of the below macros:

`(report-error sentry-reporter exception "message")` for reporting error

`(report-warn sentry-reporter exception "message")` for reporting warning

#### Shutting down
Sentry library provisions threads to handle async calls. This function releases the threads for graceful shutdown of application.

`(sentry-clj.async/shutdown-reporter sentry-reporter)`


## Example:

```clojure
(ns example
  (:require [sentry-clj.async :as sentry]))

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

## License
```
Copyright 2018, GO-JEK Tech <http://gojek.farm>

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
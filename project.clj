(defproject farm.gojek/sentry-clj.async "0.2.5"
  :description "An async library for pushing events to sentry"
  :url "https://github.com/gojekfarm/sentry-clj.async"
  :license {:name "Apache License, Version 2.0"
            :url  "https://www.apache.org/licenses/LICENSE-2.0"}
  :repl-options {:host "0.0.0.0"
                 :port 1337}
  :dependencies [[com.google.guava/guava "23.0"]
                 [org.clojure/clojure "1.9.0"]
                 [org.clojure/tools.logging "0.4.0"]
                 [raven-clj "1.5.1"]])


(defproject finagle-clojure/thrift "0.5.2-AMP"
  :description "A light wrapper around finagle-thrift for Clojure"
  :url "https://github.com/twitter/finagle-clojure"
  :scm {:name "git" :url "https://github.com/finagle/finagle-clojure"}
  :license {:name "Apache License, Version 2.0"
            :url "https://www.apache.org/licenses/LICENSE-2.0"}

  :pedantic? :abort

  :repositories
  [["twitter" "https://maven.twttr.com/"]]

  :plugins
  [[lein-midje "3.1.3"]
   [lein-finagle-clojure "0.5.2-AMP" :hooks false]]

  :dependencies
  [[finagle-clojure/core "0.5.2-AMP"]
   [com.twitter/finagle-thrift_2.10 "6.35.0"]
   [org.apache.thrift/libthrift "0.9.1"]

   ; Version overrides
   [org.apache.httpcomponents/httpclient "4.3.4"]
   [org.apache.httpcomponents/httpcore "4.3.2"]]

  :java-source-paths ["test/java"]
  :test-paths ["test/clj/"]
  :jar-exclusions [#"test"]

  :finagle-clojure
  {:thrift-source-path "test/resources"
   :thrift-output-path "test/java"}

  :profiles
  {:test {:dependencies [[midje "1.7.0" :exclusions [org.clojure/clojure]]]}
   :dev [:test {:dependencies [[org.clojure/clojure "1.8.0"]]}]
   :1.7 [:test {:dependencies [[org.clojure/clojure "1.7.0"]]}]
   :1.6 [:test {:dependencies [[org.clojure/clojure "1.6.0"]]}]
   :1.5 [:test {:dependencies [[org.clojure/clojure "1.5.1"]]}]
   :1.4 [:test {:dependencies [[org.clojure/clojure "1.4.0"]]}]})

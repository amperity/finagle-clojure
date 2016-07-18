(defproject finagle-clojure/core "0.5.2-AMP"
  :description "A light wrapper around Finagle & Twitter Util for Clojure"
  :url "https://github.com/twitter/finagle-clojure"
  :scm {:name "git" :url "http://github.com/finagle/finagle-clojure"}
  :license {:name "Apache License, Version 2.0"
            :url "https://www.apache.org/licenses/LICENSE-2.0"}

  :pedantic? :abort

  :plugins
  [[lein-midje "3.1.3"]]

  :dependencies
  [[com.twitter/finagle-core_2.10 "6.35.0"]
   [org.clojure/algo.monads "0.1.5"]

   ; Version overrides
   [com.google.guava/guava "19.0"]
   [commons-codec "1.10"
    :exclusions [com.sun.jmx/jmxri com.sun.jdmk/jmxtools javax.jms/jms]]
   [org.clojure/tools.macro "0.1.5"]]

  :profiles
  {:test {:dependencies [[midje "1.7.0" :exclusions [org.clojure/clojure]]
                         [criterium "0.4.3"]]}
   :dev [:test {:dependencies [[org.clojure/clojure "1.8.0"]]}]
   :1.7 [:test {:dependencies [[org.clojure/clojure "1.7.0"]]}]
   :1.6 [:test {:dependencies [[org.clojure/clojure "1.6.0"]]}]
   :1.5 [:test {:dependencies [[org.clojure/clojure "1.5.1"]]}]
   :1.4 [:test {:dependencies [[org.clojure/clojure "1.4.0"]]}]})

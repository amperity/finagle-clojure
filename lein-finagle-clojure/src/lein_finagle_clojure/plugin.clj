(ns lein-finagle-clojure.plugin
  (:require [leiningen.finagle-clojure]
            [leiningen.core.project :as p]))

(defn hooks []
  (leiningen.finagle-clojure/activate))

(defn middleware [project]
  (if-let [thrift-source-path (get-in project [:finagle-clojure :thrift-source-path])]
    (update-in project [:source-paths] conj thrift-source-path)
    project))

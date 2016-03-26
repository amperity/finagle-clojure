(defproject lein-finagle-clojure "0.5.1-SNAPSHOT"
  :description "A lein plugin for working with finagle-clojure"
  :url "https://github.com/twitter/finagle-clojure"
  :license {:name "Apache License, Version 2.0"
            :url "https://www.apache.org/licenses/LICENSE-2.0"}
  :scm {:name "git" :url "https://github.com/finagle/finagle-clojure"}
  :min-lein-version "2.0.0"
  :repositories [["sonatype" "https://oss.sonatype.org/content/groups/public/"]
                 ["twitter" {:url "https://maven.twttr.com/" :checksum :warn}]]
  :dependencies [[com.twitter/scrooge-generator_2.10 "3.17.4-TellApart"]
                 [com.twitter/scrooge-linter_2.10 "3.17.4-TellApart"]]
  :eval-in-leiningen true)

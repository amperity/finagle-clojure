(defproject lein-finagle-clojure "0.5.3-AMP"
  :description "A lein plugin for working with finagle-clojure"
  :url "https://github.com/twitter/finagle-clojure"
  :scm {:name "git" :url "https://github.com/finagle/finagle-clojure"}
  :license {:name "Apache License, Version 2.0"
            :url "https://www.apache.org/licenses/LICENSE-2.0"}

  :min-lein-version "2.0.0"
  :pedantic? :abort

  :repositories
  [["sonatype" "https://oss.sonatype.org/content/groups/public/"]
   ["twitter" {:url "https://maven.twttr.com/" :checksum :warn}]]

  :dependencies
  [[com.twitter/scrooge-generator_2.10 "3.17.4-TellApart"]
   [com.twitter/scrooge-linter_2.10 "3.17.4-TellApart"]

   ; Version overrides
   [com.google.code.findbugs/jsr305 "2.0.1"]
   [com.google.guava/guava "19.0"]
   [org.scala-lang/scala-library "2.10.6"]
   [org.slf4j/slf4j-api "1.7.21"]]

  :eval-in-leiningen true)

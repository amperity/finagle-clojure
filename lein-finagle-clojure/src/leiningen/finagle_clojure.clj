(ns leiningen.finagle-clojure
  (:require [leiningen.javac]
            [robert.hooke]
            [clojure.java.io :as io]
            [leiningen.core.classpath :as classpath]
            [leiningen.core.main :as lein]
            [cemerick.pomegranate :as pom]
            [clojure.set :as set]
            [clojure.string :as str])
  (:import (java.io File)
           (java.net URL)))

(defn- find-thrift-files
  [project-root source-path]
  (->> source-path
    (io/file project-root)
    (file-seq)
    (filter #(and (.isFile %) (.endsWith (.getName %) ".thrift")))
    (map #(.getPath %))))

(defn- scrape-includes
  "Returns the filenames of thrift files this code tries to include."
  [thrift-code]
  (map second (re-seq #"include \"(.+\.thrift)\"" thrift-code)))

(defn- target-temp-dir
  [project]
  (io/file (:target-path project) "thrift-include"))

(defn- copy-file-to-target-temp-dir
  "Copies a thrift file we depend on into target/thrift-include"
  [project-root ^URL thrift-url filename ^File target-dir]
  (let [target-file (io/file target-dir filename)]
    (lein/info
     "Copying" (.getPath thrift-url) "to" (.getPath target-file))
    (spit target-file (slurp thrift-url))))

(defn scrooge
  "Compile Thrift definitions into Java classes using Scrooge.

  Scrooge is a Thrift compiler that generates classes with 
  Finagle appropriate interfaces (wraps Service method return values in Future).

  Scrooge also provides a Thrift linter that can be run before compilation. Lint errors will
  prevent compilation. Pass :lint as an argument to this task to enable linting.
  Additional args passed after :lint will be passed to the linter as well.
  See https://twitter.github.io/scrooge/Linter.html or run :lint with --help for more info.
  
  This task expects the following config to present on the project:

    :finagle-clojure {:thrift-source-path \"\" :thrift-output-path \"\"}

  Example usage:

    lein finagle-clojure scrooge # compiles thrift files
    lein finagle-clojure scrooge :lint # lints thrift files before compilation
    lein finagle-clojure scrooge :lint --help # shows available options for the linter
    lein finagle-clojure scrooge :lint -w # show linter warnings as well (warnings won't prevent compilation)"
  [project & options]
  (doseq [f (classpath/get-classpath project)]
    (pom/add-classpath f))
  (let [subtask (first options)
        project-root (:root project)
        source-path (get-in project [:finagle-clojure :thrift-source-path])
        raw-dest-path (get-in project [:finagle-clojure :thrift-output-path])]
    (if-not (and source-path raw-dest-path)
      (lein/info "No config found for lein-finagle-clojure, not compiling Thrift for" (:name project))
      (let [absolute-dest-path (->> raw-dest-path (io/file project-root) (.getAbsolutePath))
            thrift-files (find-thrift-files project-root source-path)
            include-filenames (set/difference (->> thrift-files
                                                   (mapcat (comp scrape-includes slurp))
                                                   set)
                                              (->> thrift-files
                                                   (map #(last (str/split % #"/")))
                                                   set))
            ^File temp-dir (target-temp-dir project)
            scrooge-args (concat ["--finagle" "--skip-unchanged" "--language" "java" "--dest" absolute-dest-path]
                                 ["-i" (.getPath temp-dir)]
                                 thrift-files)]
        (.mkdirs temp-dir)
        (doseq [filename include-filenames
                :let [resource (io/resource filename)]]
          (if-not resource
            (lein/abort (str "Aborting, could not find thrift source file: " filename))
            (copy-file-to-target-temp-dir project-root resource filename temp-dir)))
        (when (= subtask ":lint")
          (let [default-args ["--disable-rule" "Namespaces"]
                additional-args (rest options)
                linter-args (concat default-args additional-args thrift-files)]
            (lein/info "Linting Thrift files:" thrift-files)
            (com.twitter.scrooge.linter.Main/main (into-array String linter-args))))
        (lein/info "Compiling Thrift files:" thrift-files)
        (lein/debug "Calling scrooge with parameters:" scrooge-args)
        (com.twitter.scrooge.Main/main (into-array String scrooge-args))))))

(defn javac-hook
  [f project & args]
  (scrooge project)
  (apply f project args))

(defn finagle-clojure
  "Adds a hook to lein javac to compile Thrift files first."
  {:help-arglists '([scrooge])
   :subtasks [#'scrooge]}
  [project subtask & args]
  (case subtask
    "scrooge" (apply scrooge project args)))

(defn activate []
  (robert.hooke/add-hook #'leiningen.javac/javac #'javac-hook))

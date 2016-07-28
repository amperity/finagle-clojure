(ns leiningen.finagle-clojure
  (:require
    [clojure.java.io :as io]
    [clojure.set :as set]
    [clojure.string :as str]
    [leiningen.core.classpath :as classpath]
    [leiningen.core.main :as lein]
    [leiningen.javac]
    [robert.hooke])
  (:import
    java.io.File
    (java.util.jar
      JarEntry
      JarFile)))


(defn- find-thrift-files
  "Returns a sequence of paths to Thrift files in the given source directory."
  [source-path]
  (->>
    (io/file source-path)
    (file-seq)
    (filter (fn [^File f] (and (.isFile f) (.endsWith (.getName f) ".thrift"))))
    (map #(.getPath ^File %))))


(defn- find-thrift-entries
  "Reads JAR file entries to locate Thrift files. Returns a sequence of maps
  corresponding to the files, with `:source`, `:path`, and `:content` entries."
  [jar-path]
  (let [jar (JarFile. (io/file jar-path))]
    (->> (.entries jar)
         (iterator-seq)
         (filter #(re-seq #"\.thrift$" (.getName ^JarEntry %)))
         (map (fn [^JarEntry entry]
                {:source (.getName jar)
                 :path (.getName entry)
                 :content (slurp (.getInputStream jar entry))})))))


(defn- find-thrift-dependencies
  "Reads JAR files from the project's classpath to locate included Thrift
  files. Returns a map from path strings to the corresponding entry. Prints a
  warning if duplicate files are discovered."
  [project]
  (->> (classpath/get-classpath project)
       (filter (partial re-seq #"[^/]+\.jar$"))
       (mapcat find-thrift-entries)
       (group-by :path)
       (map (fn [[path entries]]
              (when (< 1 (count entries))
                (lein/warn "WARN: Thrift source path" path
                           "provided by multiple source JARs:"
                           (str/join " " (map :source entries))))
              [path (first entries)]))
       (into {})))


(defn- scrape-includes
  "Returns the filenames of thrift files this code tries to include."
  [thrift-code]
  (map second (re-seq #"include \"(.+\.thrift)\"" thrift-code)))


(defn- external-includes
  "Returns the paths of included thrift files which are not in the collection
  of source thrift file paths given."
  [root source-paths thrift-dependencies]
  (let [include-paths (->> source-paths
                           (mapcat (comp scrape-includes slurp))
                           (set))
        relative-sources (->> source-paths
                              (map #(subs % (inc (count (str root)))))
                              (set))
        external-deps (set/difference include-paths relative-sources)]
    ;; For each of those external dependencies, recursively scrape
    ;; THEM to find any transitive dependencies. Breadth-first search.
    (loop [q (into clojure.lang.PersistentQueue/EMPTY external-deps)
           result []]
      (if (empty? q)
        result
        (let [filename (peek q)
              {:keys [content]} (thrift-dependencies filename)]
          (recur (into (pop q)
                       (when content
                         ;; If content is nil, we have a problem, but
                         ;; the scrooge task will abort accordingly
                         (scrape-includes content)))
                 (conj result filename)))))))


(defn- target-include-dir
  "Return a file directory which thrift files should be placed in."
  ^File
  [project]
  (io/file (:target-path project) "thrift-include"))


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
  (let [subtask (first options)
        project-root (:root project)
        source-path (get-in project [:finagle-clojure :thrift-source-path])
        raw-dest-path (get-in project [:finagle-clojure :thrift-output-path])]
    (if-not (and source-path raw-dest-path)
      (lein/debug "No config found for lein-finagle-clojure, not compiling Thrift for" (:name project))
      (let [absolute-source-path (.getAbsolutePath (io/file project-root source-path))
            absolute-dest-path (.getAbsolutePath (io/file project-root raw-dest-path))
            thrift-sources (find-thrift-files absolute-source-path)
            thrift-deps (find-thrift-dependencies project)
            include-paths (external-includes absolute-source-path thrift-sources thrift-deps)
            include-dir (target-include-dir project)
            source-names (map #(last (str/split % #"/")) thrift-sources)
            scrooge-args (concat ["--finagle" "--skip-unchanged" "--language" "java" "--dest" absolute-dest-path]
                                 (when (seq include-paths)
                                   ["-i" (.getPath include-dir)])
                                 thrift-sources)]
        (when (seq include-paths)
          (.mkdirs include-dir))
        (doseq [path include-paths
                :let [thrift-entry (get thrift-deps path)]]
          (when-not thrift-entry
            (lein/abort (str "Aborting, could not find included thrift file: " path)))
          (lein/info "Including thrift source:" path "from"
                     (-> thrift-entry :source (str/split #"/") last))
          (spit (io/file include-dir path) (:content thrift-entry)))
        (when (= subtask ":lint")
          (let [default-args ["--disable-rule" "Namespaces"]
                additional-args (rest options)
                linter-args (concat default-args additional-args thrift-sources)]
            (lein/info "Linting Thrift files:" (str/join " " source-names))
            (com.twitter.scrooge.linter.Main/main (into-array String linter-args))))
        (lein/info "Compiling Thrift files:" (str/join " " source-names))
        (lein/debug "Calling scrooge with parameters:" (str/join " " scrooge-args))
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

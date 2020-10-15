(ns blog.util.runner
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [clojure.test]
            [clojure.tools.cli :as cli]
            [cloverage.args :as cov-args]
            [cloverage.coverage :as cloverage]
            [eftest.report :as report]
            [eftest.runner :as runner]))

(def ^:private EFTEST-JUNIT-REPORT-FN-NAME "eftest.report.junit/report")


(defn- parse-boolean-str
  [value]
  (case (str/lower-case value)
    "true" true
    "false" false
    nil))


(defn- parse-multithread?-option
  [value]
  {:post [(contains? #{nil true false :vars :namespaces} %)]}
  (when (some? value)
    (if-some [bool (parse-boolean-str value)]
      bool
      (#'cov-args/parse-kw-str value))))


(defn- resolve-str-option
  "Resolve symbol represented as string option.

  Require according namespace when report function is junit."
  [value]
  (when (= value EFTEST-JUNIT-REPORT-FN-NAME)
    (require '[eftest.report.junit]))
  (-> value
    (#'cov-args/parse-sym-str)
    (resolve)))


(def ^:private origin-arguments
  [["--[no-]coverage"
    "Run test runner without coverage instrumenting."
    :default true]])


(def ^:private eftest-arguments
  [["--eftest-fail-fast?"
    "If true, stop after first failure or error."
    :default false
    :parse-fn parse-boolean-str]
   ["--eftest-capture-output?"
    "If true, catch test output and print it only
    if the test fails (defaults to true)."
    :default true
    :parse-fn parse-boolean-str]
   ["--eftest-multithread?"
    "One of: true, false, :namespaces or :vars (defaults to
    true). If set to true, namespaces and vars are run in
    parallel; if false, they are run in serial. If set to
    :namespaces, namespaces are run in parallel but the vars
    in those namespaces are run serially. If set to :vars,
    the namespaces are run serially, but the vars inside run
    in parallel."
    :default true
    :parse-fn parse-multithread?-option]
   ["--eftest-thread-count"
    "The number of threads used to run the tests in parallel
    (as per :multithread?). If not specified, the number
    reported by java.lang.Runtime.availableProcessors (which
    is not always accurate) *plus two* will be used."
    :parse-fn #(Integer/parseInt %)]
   ["--eftest-randomize-seed"
    "The random seed used to deterministically shuffle
    test namespaces before running tests (defaults to 0)."
    :parse-fn #(Integer/parseInt %)]
   ["--eftest-report"
    "The test reporting function to use
    (defaults to eftest.report.progress/report)."
    :parse-fn resolve-str-option]
   ["--eftest-report-to-file"
    "Redirect reporting output to a file.
    (no default value)."
    :parse-fn str]
   ["--eftest-test-warn-time"
    "Print a warning for any test that exceeds this
    time (measured in milliseconds)."
    :parse-fn #(Integer/parseInt %)]])


(def ^:private updated-default-arguments
  [["-r" "--runner"
    ; TODO: update to actual runner name if needed!
    "Specify which test runner to use. Default runner: `eftest`."
    ; TODO: update to actual runner namespace!
    :default :blog.util
    :parse-fn #'cov-args/parse-kw-str]])


(defn- arg-to-update?
  [arg]
  (let [arg-names-to-filter (set (map second updated-default-arguments))]
    (contains? arg-names-to-filter (second arg))))


(defn- parse-args
  "Combine cloverage and eftest arguments' definition and parse given cli args."
  [args]
  (let [arguments (->> cov-args/arguments
                       (remove arg-to-update?)
                       (concat origin-arguments
                               eftest-arguments
                               updated-default-arguments))]
    (#'cov-args/fix-opts (apply cli/cli args arguments) {})))


(def ^:private EFTEST-OPTS-PREFIX "eftest-")


(defn- starts-with-prefix?
  [opt-key]
  (let [opt-name (name opt-key)]
    (str/starts-with? opt-name EFTEST-OPTS-PREFIX)))


(defn- eftest-keymap
  [key-with-prefix]
  (let [result-key (-> key-with-prefix
                       (name)
                       (str/replace EFTEST-OPTS-PREFIX "")
                       (keyword))]
    [key-with-prefix result-key]))


(defn- assoc-eftest-opts
  [opts]
  (let [eftest-keys-keymap (->> (keys opts)
                                (filter starts-with-prefix?)
                                (map eftest-keymap)
                                (into {}))
        origin-keys (keys eftest-keys-keymap)
        eftest-opts (-> opts
                        (select-keys origin-keys)
                        (set/rename-keys eftest-keys-keymap))]
    (-> (apply dissoc opts origin-keys)
        (assoc :eftest-opts eftest-opts))))


(defn- assoc-eftest-report-fn
  "Assoc to eftest opts a report function in respect of `retport-to-file` param."
  [opts]
  (let [report-fn (get-in opts [:eftest-opts :report])
        report-to-file-path (get-in opts [:eftest-opts :report-to-file])]
    (if (and (some? report-fn)
             (some? report-to-file-path))
      (assoc-in opts
                [:eftest-opts :report]
                (report/report-to-file report-fn report-to-file-path))
      opts)))


(defn- require-ns
  "Require and return namespace by given string."
  [ns-str]
  (let [ns-sym (symbol ns-str)]
    (require ns-sym)
    (find-ns ns-sym)))


(defn- find-test-namespaces
  "Parse cloverage test paths options and return seq of testing namespaces."
  [{:keys [test-ns-path extra-test-ns test-ns-regex] :as _opts}]
  (if-some [test-namespaces (->> (cloverage/find-nses test-ns-path test-ns-regex)
                                 (concat extra-test-ns)
                                 (set)
                                 (map require-ns)
                                 (seq))]
    test-namespaces
    (throw
      (IllegalArgumentException.
        (str/join "\n"
          ["Test namespaces not found."
           "Please, setup at least one of options: `test-ns-path`, `extra-test-ns` or `test-ns-regex`."])))))


(defn- assoc-eftest-test-namespaces
  [opts]
  (if-some [test-namespaces (find-test-namespaces opts)]
    (assoc-in opts [:eftest-opts :test-namespaces] test-namespaces)
    opts))


(defn run-tests
  "Run eftest and parse args for options."
  [{:keys [eftest-opts] :as opts}]
  (let [test-namespaces (if (seq (:test-namespaces eftest-opts))
                          (:test-namespaces eftest-opts)
                          ; Find namespaces when running via `lein-cloverage`
                          (find-test-namespaces opts))
        eftest-opts* (if (contains? eftest-opts :test-namespaces)
                       (dissoc eftest-opts :test-namespaces)
                      {})
        test-vars (runner/find-tests test-namespaces)]
    (runner/run-tests test-vars eftest-opts*)))


(defn -main
  [& args]
  (let [parsed-opts (parse-args args)
        coverage? (get-in parsed-opts [0 :coverage])
        opts (update-in parsed-opts [0]
                        (comp assoc-eftest-test-namespaces
                              assoc-eftest-report-fn
                              assoc-eftest-opts))]
    (if coverage?
      (cloverage/run-main opts {})
      (run-tests (first opts)))))


; TODO: remove!
(comment
  (let [args ["-p" "src"
              "-s" "test"]
              ;"--runner" ":blog.util"
              ;"--eftest-test-warn-time" "500"
              ;"--eftest-randomize-seed" "2"
              ;"--eftest-multithread?" ":vars"
              ;"--eftest-fail-fast?" "true"
              ;"--no-coverage"]
        ns-str "blog.articles-test"]
              ;"--eftest-report" "eftest.report.progress/report"]
        ;opts (parse-args args)]
    ;(runner/find-tests [(symbol "blog.articles-test")])))
    ;(the-ns (symbol ns-str))
    (ns-name (symbol ns-str))))
    ;(ns-map (find-ns (symbol ns-str)))
    ;(namespace (symbol ns-str))))
    ;(apply -main args)))
    ;    eftest-arguments [["--eftest-test-warn-time"
    ;                       "Print a warning for any test that exceeds this time (measured in milliseconds)"
    ;                       :parse-fn #(Integer/parseInt %)]]
    ;    cloverage-arguments (concat cov-args/arguments eftest-arguments)]
    ;(#'cov-args/fix-opts (apply cli/cli args cloverage-arguments) {})
    ;(select-eftest-opts)))
    ;(first opts)))
    ;(eftest-opts (first opts))
    ;(with-redefs [cov-args/valid valid-with-eftest]
    ;(-> (assoc-eftest-opts (first opts))
    ;    :eftest-opts
    ;    :report
    ;    (type))
    ;(report/report-to-file #'eftest.report.junit/report "target/eftest/junit.xml")
    ;(-> (first opts)
    ;    keys
    ;    first
    ;    name)))
    ;(-> {:a 1 :b 2 :c 3}
    ;  (#(apply dissoc % [:a :b])))))
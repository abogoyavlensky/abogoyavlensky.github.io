(ns blog.util.runner
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [clojure.test]
            [clojure.tools.cli :as cli]
            [cloverage.args :as cov-args]
            [cloverage.coverage :as cloverage]
            [eftest.report :as report]
            [eftest.runner :as runner]))


(def ^:private DEFAULT-TEST-PATH "test")


(defn run-tests
  "Run eftest and parse args for options."
  [{:keys [eftest-opts test-ns-path]}]
  (let [eftest-opts (or eftest-opts {})
        test-path (if (seq test-ns-path)
                    test-ns-path
                    DEFAULT-TEST-PATH)
        test-vars (runner/find-tests test-path)]
    (runner/run-tests test-vars eftest-opts)))


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
  "Resolve symbol represented as string option."
  [value]
  (-> value
    (#'cov-args/parse-sym-str)
    (resolve)))


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
                       (concat eftest-arguments
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


(defn -main
  [& args]
  (let [
        ;cloverage-opts (cov-args/parse-args args {})
        parsed-opts (parse-args args)
        ;eftest-opts {:fail-fast? true
        ;             :multithread? :vars}
        ;eftest-opts (eftest-opts (first opts))]
        ;opts (assoc-eftest-opts parsed-opts)
        ;opts (update-in cloverage-opts [0] #(merge % {:eftest-opts eftest-opts}))
        opts (update-in parsed-opts [0]
                        (comp assoc-eftest-report-fn
                              assoc-eftest-opts))]
        ;report-fn (multi-report progress/report)
        ;opts (assoc-in opts [0 :eftest-opts :report] report-fn)
        ;opts (assoc-in opts [0 :eftest-opts :report] progress/report)
        ;opts (assoc-in opts [0 :eftest-opts :report] (resolve 'eftest.report.progress/report))]
    ;(prn report-fn)
    ;(prn report-to-file-path)
    (prn (-> opts first))
    (prn (-> opts first :runner))
    (prn (-> opts first :runner type))
    (cloverage/run-main opts {})))



(comment
  (let [args ["-p" "src"
              "-s" "test"
              "--runner" ":blog.util"
              "--eftest-test-warn-time" "500"
              "--eftest-randomize-seed" "2"
              "--eftest-multithread?" ":vars"
              "--eftest-fail-fast?" "true"
              "--eftest-report" "eftest.report.progress/report"]
        opts (parse-args args)]
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
    (report/report-to-file #'eftest.report.junit/report "target/eftest/junit.xml")))
    ;(-> (first opts)
    ;    keys
    ;    first
    ;    name)))
    ;(-> {:a 1 :b 2 :c 3}
    ;  (#(apply dissoc % [:a :b])))))

(ns blog.util.runner
  (:require [clojure.test]
            [clojure.string :as str]
            [clojure.set :as set]
            [clojure.tools.cli :as cli]
            [cloverage.args :as cov-args]
            [cloverage.coverage :as cloverage]
            [eftest.report :as report]
            [eftest.report.progress :as progress]
            [eftest.report.junit :as ju]
            [eftest.runner :as runner]))


(def ^:private DEFAULT-TEST-PATH "test")
(def ^:private OUTPUT-JUINT-PATH "target/eftest/junit.xml")


; TODO: remomve!
(def output-path
  "target/eftest/junit.xml")


(defn- multi-report
  "Accepts n reporting functions, returns a reporting function that will call
  them all for their side effects and return nil."
  [first-fn & rest-fns]
  (fn [event]
    ;; Run the first reporting function normally
    (first-fn event)

    ;; Now bind the clojure.test/*report-counters* to nil and then run the rest
    ;; of the functions, so as to avoid double-counting of the assertions,
    ;; errors, and failures as per https://github.com/weavejester/eftest/issues/23
    (binding [clojure.test/*report-counters* nil]
      (doseq [report rest-fns]
        (report event)))))


(defn run-tests
  "Run eftest and parse args for options."
  [{:keys [eftest-opts test-ns-path]}]
  (let [eftest-opts (or eftest-opts {})
        test-path (if (seq test-ns-path)
                    test-ns-path
                    DEFAULT-TEST-PATH)
        test-vars (runner/find-tests test-path)]
    (runner/run-tests test-vars eftest-opts)))


;(def opts
;  (let [report-to-file-fn (report/report-to-file ju/report output-path)
;        report-fn (multi-report progress/report report-to-file-fn)]
;    {
;     ;:report report-fn
;
;     ;; :multithread? supports a few different values; I tested the other supported values
;     ;; (:namespaces and `true`) but they caused concurrency problems with rendering. If/when we
;     ;; have threadsafe rendering we might want to revisit this setting.
;     ;;
;     ;; docs: https://github.com/weavejester/eftest/#multithreading
;     :multithread? :vars
;     ;:multithread? true
;
;     ;:thread-count (thread-count)
;
;     ;; We have *lots* of tests that take too damn long.
;     ;:test-warn-time 10 ; millis
;
;     ;; Of course our test suite takes way too damn long.
;     :fail-fast? true
;
;     :report report-fn}))
;     ;:report eftest.report.junit/report
;     ;:report-to-file "target/eftest/junit.xml"}))


;(defn run-tests
;  [opts]
;  (runner/run-tests (runner/find-tests test-dir) opts))


;(defn -main
;  [& args]
;  (let [results (run-tests)
;        unsuccessful-tests (->> results
;                                ((juxt :error :fail))
;                                (reduce +))
;        exit-code (if (zero? unsuccessful-tests) 0 1)]
;    (shutdown-agents)
;    (System/exit exit-code)))

(def ^:private eftest-arguments
  [["--eftest-test-warn-time"
    "Print a warning for any test that exceeds this time (measured in milliseconds)"
    :parse-fn #(Integer/parseInt %)]
   ["--eftest-randomize-seed"
    "The random seed used to deterministically shuffle test namespaces before running tests (defaults to 0)"
    :parse-fn #(Integer/parseInt %)]])


(defn- parse-args
  "Combine cloverage and eftest arguments' definition and parse given cli args."
  [args]
  (let [arguments (concat cov-args/arguments eftest-arguments)]
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
        opts (update-in parsed-opts [0] assoc-eftest-opts)]
    ;(prn opts)))
    (cloverage/run-main opts {})))


(comment
  (let [args ["-p" "src" "-s" "test" "--runner" ":blog.util"
              "--eftest-test-warn-time" "500"
              "--eftest-randomize-seed" "2"]
              ;"--eftest-fail-fast"]
              ;"--eftest-multithread?" ":vars"]
        opts (parse-args args)]
    ;    eftest-arguments [["--eftest-test-warn-time"
    ;                       "Print a warning for any test that exceeds this time (measured in milliseconds)"
    ;                       :parse-fn #(Integer/parseInt %)]]
    ;    cloverage-arguments (concat cov-args/arguments eftest-arguments)]
    ;(#'cov-args/fix-opts (apply cli/cli args cloverage-arguments) {})
    ;(select-eftest-opts)))
    ;(first opts)))
    ;(eftest-opts (first opts))
    (assoc-eftest-opts (first opts))))
    ;(-> (first opts)
    ;    keys
    ;    first
    ;    name)))
    ;(-> {:a 1 :b 2 :c 3}
    ;  (#(apply dissoc % [:a :b])))))
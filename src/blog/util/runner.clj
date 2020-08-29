(ns blog.util.runner
  (:require [clojure.test]
            [eftest.report :as report :refer [report-to-file]]
            [eftest.report.progress :as progress]
            [eftest.report.junit :as ju]
            [eftest.runner :as runner :refer [find-tests]]))


(def test-dir "test")


(def output-path
  "This is optimized for CircleCI: https://circleci.com/docs/2.0/configuration-reference/#store_test_results"

  "target/eftest/junit.xml")


(defn- multi-report
  "Accepts n reporting functions, returns a reporting function that will call
  them all for their side effects and return nil. I tried to just use juxt but
  it didn’t work. Maybe because some of the reporting functions provided by
  eftest are multimethods, I don’t know."
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


(def opts
  (let [report-to-file-fn (report-to-file ju/report output-path)
        report-fn (multi-report progress/report report-to-file-fn)]
    {
     ;:report report-fn

     ;; :multithread? supports a few different values; I tested the other supported values
     ;; (:namespaces and `true`) but they caused concurrency problems with rendering. If/when we
     ;; have threadsafe rendering we might want to revisit this setting.
     ;;
     ;; docs: https://github.com/weavejester/eftest/#multithreading
     :multithread? :vars
     ;:multithread? true

     ;:thread-count (thread-count)

     ;; We have *lots* of tests that take too damn long.
     ;:test-warn-time 10 ; millis

     ;; Of course our test suite takes way too damn long.
     :fail-fast? true

     :report report-fn}))
     ;:report eftest.report.junit/report
     ;:report-to-file "target/eftest/junit.xml"}))


(defn run-tests
  []
  (runner/run-tests (find-tests test-dir) opts))


(defn -main []
  (let [results (run-tests)
        unsuccessful-tests (->> results
                                ((juxt :error :fail))
                                (reduce +))
        exit-code (if (zero? unsuccessful-tests) 0 1)]
    (shutdown-agents)
    (System/exit exit-code)))

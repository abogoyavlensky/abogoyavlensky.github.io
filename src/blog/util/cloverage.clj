(ns blog.util.cloverage
  "An adapter that enables Cloverage to use this project’s custom test-runner
  (which is really just a test-runner-runner that runs eftest with some
  specific options)."
  (:require [cloverage.coverage :as cov]
            [cloverage.args :as cov-args]
            [blog.util.runner :as runner]))

;; This is based on https://github.com/circleci/circleci.test/blob/master/src/circleci/test/cloverage.clj
;; and the structure of the below function, and the comment that precedes it, come from that file.

;; This is a copy of the clojure.test runner which ships with cloverage
;; but with clojure.test swapped out with fc4.test-runner.runner's run-tests.
(defmethod cov/runner-fn :blog.util
  [{}]
  (fn [nses]
    (let [results (runner/run-tests)]
      (apply require (map symbol nses))
      {:errors (reduce + ((juxt :error :fail)
                          results))})))





(comment
  (let [args [;"-m" "cloverage.coverage"
              "-n" "blog.*"
              "-e" "blog.*-test"
              "--test-ns-regex" "blog.+\\-test$"
              "-e" "blog.util.*"
              "--runner" ":blog.util"
              "--eftest" ":foo"]
              ;"-r" ":default"]
        opts {}]

    (binding [cov/*exit-after-test* false]
      (-> args
          (cov-args/parse-args opts)))))
          ;(cov/run-main opts)))))

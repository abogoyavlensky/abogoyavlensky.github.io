(ns blog.util.cloverage
  "Wrapper for running `cloverage` with `eftest` test runner."
  (:require [cloverage.coverage :as cov]
            [cloverage.args :as cov-args]
            [blog.util.runner :as runner]))

; This is based on https://github.com/circleci/circleci.test/blob/master/src/circleci/test/cloverage.clj

(defmethod cov/runner-fn :blog.util
  [args]
  (fn [namespaces]
    (let [results (runner/run-tests args)]
      (apply require (map symbol namespaces))
      {:errors (reduce + ((juxt :error :fail) results))})))


; TODO: remove!
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

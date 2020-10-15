(ns blog.util.cloverage
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

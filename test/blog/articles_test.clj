(ns blog.articles-test
  (:require [clojure.test :refer [deftest is]]
            [blog.articles :as articles]))


(deftest test-humanize-date
  (is (= "August 28, 2020" (#'articles/humanize-date "2020-08-28"))))

(ns blog.articles-test
  (:require [clojure.test :refer [deftest is]]
            [blog.articles :as articles]))


(deftest test-humanize-date-1
  (is (= "August 28, 2020" (#'articles/humanize-date "2020-08-28"))))


(deftest test-humanize-date-2
  (is (= "January 29, 2020" (#'articles/humanize-date "2020-01-29"))))


(deftest test-humanize-date-3
  (is (= "May 30, 2020" (#'articles/humanize-date "2020-05-30"))))


(deftest test-articles-list-data
  (is (= [{:id 1,
           :slug "clojure-formatting-cljstyle",
           :title "Clojure formatting with cljstyle",
           :date #inst"2020-08-27T21:00:00.000-00:00",
           :keywords ["clojure" "fmt" "cljstyle" "clj" "formatting"],
           :description "Clojure formatting in practice using the cljstyle tool",
           :date-str "August 28, 2020"}]
         (articles/articles-list-data
           {:articles
            [{:id 1
              :slug "clojure-formatting-cljstyle"
              :title "Clojure formatting with cljstyle"
              :date "2020-08-28"
              :keywords ["clojure" "fmt" "cljstyle" "clj" "formatting"]
              :description "Clojure formatting in practice using the cljstyle tool"}]}))))

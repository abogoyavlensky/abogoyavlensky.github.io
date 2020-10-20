(ns blog.articles-test
  (:require [clojure.test :refer [deftest is]]
            [blog.articles :as articles]))


(deftest test-humanize-date-ok
  (is (= "August 28, 2020" (#'articles/humanize-date "2020-08-28"))))


(deftest test-articles-list-data-ok
  (is (= [{:id 1,
           :slug "clojure-formatting-cljstyle",
           :title "Clojure formatting with cljstyle",
           :date #inst"2020-08-28T00:00:00.000-00:00",
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

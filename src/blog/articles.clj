(ns blog.articles
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [markdown.core :as markdown]))


(def ^:private META-DATA-PATH "public/data/meta.edn")


(defn meta-data
  []
  (-> META-DATA-PATH
      (io/resource)
      (slurp)
      (edn/read-string)))


(defn articles-list-data
  [data]
  (->> data
       :articles
       (sort-by :id >)
       (map #(select-keys % [:title :slug :date]))))


; TODO: remove
(comment
  (let [data (meta-data)]
    (articles-list-data data)))

(ns blog.articles
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [markdown.core :as markdown]
            [tick.alpha.api :as t]))


(def ^:private META-DATA-PATH "data/meta.edn")
(def ^:private ARTICLE-DETAIL-PATH "data/articles/%s.md")


(defn- humanize-date
  [date-str]
  (t/format (t/formatter "MMMM d, yyyy") (t/date date-str)))


(defn meta-data
  []
  (-> META-DATA-PATH
      (io/resource)
      (slurp)
      (edn/read-string)))


(defn articles-list-data
  [site-data]
  (->> site-data
       :articles
       (sort-by :id >)
       (map (comp #(select-keys % [:title :slug :date])
                  #(update % :date humanize-date)))))


(defn- read-article-md-file
  [slug]
  (-> (format ARTICLE-DETAIL-PATH slug)
      (io/resource)
      (slurp)
      (markdown/md-to-html-string)))


(defn article-detail-data
  [slug articles-data]
  (let [article (->> articles-data
                    (filter #(= slug (:slug %)))
                    (first))
        text-md (-> (:slug article)
                    (read-article-md-file))]
    (assoc article :text text-md)))



; TODO: remove
(comment
  (let [site-data (meta-data)
        articles-data (articles-list-data site-data)
        slug "test-draft"]
    ;(articles-list-data data)
    ;(article-detail-data "test-draft" articles-data)))
    ;(article-file slug)))
    ;articles-data))
    (->> articles-data)))
        ;(map #(update % :date humanize-date)))))
        ;first
        ;:date
        ;(humanize-date))))

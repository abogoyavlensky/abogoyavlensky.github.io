(ns blog.articles
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.edn :as edn]
            [tick.alpha.api :as t]))


(def ^:private RESOURCES-PATH "resources/")
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
    (map #(assoc % :date-str (humanize-date (:date %))))
    (map #(update % :date (fn [x]
                            (-> (str x "T00:00:00.000-00:00")
                              (t/instant)
                              (t/inst)))))))


; TODO: optimize html generation,
; ideally fix `markdown-clj` or create new md->html generator in clojure.
; TODO: try clarktown clj lib!
(defn- md->html
  "Generate html from markdown using external tool.

  Do not use `markdown-clj` cause it generates wrong code blocks if
  inside them there are some md symbols `-`, `+` etc."
  [md-file-path]
  (shell/sh "docker" "compose" "run" "marked" md-file-path))


(defn- read-article-md-file
  [slug]
  (->> (format ARTICLE-DETAIL-PATH slug)
    (str RESOURCES-PATH)
    (md->html)
    :out))


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
    ;(->> articles-data)
    ;(shell/sh "make" "marked")))
    ;(shell/sh "docker-compose" "run" "marked" "resources/data/articles/test-draft.md")))
    (read-article-md-file slug)))

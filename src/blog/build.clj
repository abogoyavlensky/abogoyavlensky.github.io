(ns blog.build
  (:require [clojure.java.io :as io]
            [blog.articles :as articles]
            [blog.app :as app]
            [digest :as digest]))


(def ^:private CSS-PATH "resources/public/css/")
(def ^:private CSS-PROD-ORIGIN "output-prod.css")
(def ^:private CSS-PROD-HASH "output.%s.css")


(defn hash-css-prod
  "`build-index` and `make css-prod` should be ran before"
  []
  (let [css-prod-origin (str CSS-PATH CSS-PROD-ORIGIN)
        css-hash (digest/md5 (io/as-file css-prod-origin))
        css-hashed-name (format CSS-PROD-HASH css-hash)
        css-hashed-path (str CSS-PATH css-hashed-name)]
    (.renameTo (io/file css-prod-origin) (io/file css-hashed-path))
    css-hashed-name))




(defn- create-dir
  [path]
  (.mkdir (java.io.File. path)))


(defn- build-index
  [css-hashed-name]
  (spit "dist/index.html" (app/index nil css-hashed-name)))


(defn- build-blog
  [css-hashed-name]
  (let [site-data (articles/meta-data)
        articles-data (articles/articles-list-data site-data)
        base-dir "dist/blog"]
    (create-dir base-dir)
    (doseq [article articles-data
            :let [article-path (str base-dir "/" (:slug article))]]
      (create-dir article-path)
      (spit (str article-path "/index.html")
            (app/article-detail (:slug article) css-hashed-name)))))


(defn -main [& _args]
  (create-dir "dist")
  (let [css-hashed-name (hash-css-prod)]
    (build-index css-hashed-name)
    (build-blog css-hashed-name)))


; TODO: remove!
(comment
  (let [_ 1]
    ;(create-assets-dir)))
    ;(create-dist-dir)
    ;(build-index)))
   ;(app/index nil)))
   ; (hash-css-prod)))
    (hash-css-prod nil)))

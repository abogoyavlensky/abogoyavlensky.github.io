(ns blog.build
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
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


(defn- build-projects
  [css-hashed-name]
  (let [dir "dist/projects"]
    (create-dir dir)
    (spit (str dir "/index.html") (app/projects nil css-hashed-name))))


(defn- build-about
  [css-hashed-name]
  (let [dir "dist/about"]
    (create-dir dir)
    (spit (str dir "/index.html") (app/about nil css-hashed-name))))


(defn- build-not-found
  [css-hashed-name]
  (spit "dist/404.html" (app/not-found nil css-hashed-name)))


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
    (build-blog css-hashed-name)
    (build-projects css-hashed-name)
    (build-about css-hashed-name)
    (build-not-found css-hashed-name)))


; Generate html for postcss with common html tags
(comment
  (let [tags ["html" "body" "div" "a" "ul" "ol" "li" "article" "script" "title" "blockquote"
              "br" "b" "font" "i" "input" "textarea" "link" "meta" "head" "header" "footer"
              "span" "p" "h1" "h2" "h3" "h4" "h5" "h6" "strong" "sub" "sup" "from"
              "button" "select" "label" "option" "iframe" "img" "svg" "audio" "source" "track"
              "video" "link" "nav" "table" "caption" "th" "tr" "td" "thead" "tbody" "col" "style"
              "section" "main" "article" "base" "script" "pre" "code"]]
    (->> tags
        (map #(format "<%s>" %))
        (str/join #""))))

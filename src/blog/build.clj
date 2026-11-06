(ns blog.build
  (:require [clojure.java.io :as io]
            [blog.articles :as articles]
            [blog.app :as app]
            [blog.og-card :as og-card]
            [digest :as digest])
  (:import (java.awt.image BufferedImage)
           (javax.imageio ImageIO)))


(def ^:private CSS-PATH "resources/public/css/")
(def ^:private CSS-PROD-ORIGIN "output-prod.css")
(def ^:private CSS-PROD-HASH "output.%s.css")
(def ^:private IMAGES-PATH "resources/public/images")
(def ^:private OG-CARDS-PATH "dist/assets/images/og")


#_{:clj-kondo/ignore [:unresolved-var]}


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


(defn- build-not-found
  [css-hashed-name]
  (spit "dist/404.html" (app/not-found nil css-hashed-name)))


(defn- build-feed
  []
  (spit "dist/feed.xml" (app/feed nil)))


(defn- build-sitemap
  []
  (spit "dist/sitemap.xml" (app/sitemap nil)))


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


(defn- check-og-override!
  "A custom image has to match the size declared in the meta tags."
  [images-path og-image]
  (let [file (io/file images-path og-image)
        ^BufferedImage image (when (.isFile file)
                               (ImageIO/read file))]
    (when-not (and (some? image)
                (= og-card/WIDTH (.getWidth image))
                (= og-card/HEIGHT (.getHeight image)))
      (throw (ex-info (format "Custom :og-image must be a %dx%d image"
                        og-card/WIDTH og-card/HEIGHT)
               {:path (str file)})))))


(defn- build-og-images
  []
  (og-card/write-card! app/SITE-TAGLINE (str OG-CARDS-PATH "/site.png"))
  (doseq [article (articles/articles-list-data (articles/meta-data))]
    (if-let [og-image (:og-image article)]
      (check-og-override! IMAGES-PATH og-image)
      (og-card/write-card! (:title article)
        (str OG-CARDS-PATH "/" (:slug article) ".png")))))


(defn -main
  [& _args]
  (create-dir "dist")
  (let [css-hashed-name (hash-css-prod)]
    (build-og-images)
    (build-index css-hashed-name)
    (build-blog css-hashed-name)
    (build-projects css-hashed-name)
    (build-not-found css-hashed-name)
    (build-feed)
    (build-sitemap)))


;; Generate html for postcss with common html tags
(comment
  (let [tags ["html" "body" "div" "a" "ul" "ol" "li" "article" "script" "title" "blockquote"
              "br" "b" "font" "i" "input" "textarea" "link" "meta" "head" "header" "footer"
              "span" "p" "h1" "h2" "h3" "h4" "h5" "h6" "strong" "sub" "sup" "from"
              "button" "select" "label" "option" "iframe" "img" "svg" "audio" "source" "track"
              "video" "link" "nav" "table" "caption" "th" "tr" "td" "thead" "tbody" "col" "style"
              "section" "main" "article" "base" "script" "pre" "code"]]
    (->> tags
        (map #(format "<%s>" %))
        (clojure.string/join #""))))

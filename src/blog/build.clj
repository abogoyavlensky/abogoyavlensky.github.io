(ns blog.build
  (:require [blog.articles :as articles]
            [blog.app :as app]))


(defn create-dir
  [path]
  (.mkdir (java.io.File. path)))


(defn build-index
  []
  (spit "dist/index.html" (app/index nil)))


(defn build-blog
  []
  (let [site-data (articles/meta-data)
        articles-data (articles/articles-list-data site-data)
        base-dir "dist/blog"]
    (create-dir base-dir)
    (doseq [article articles-data
            :let [article-path (str base-dir "/" (:slug article))]]
      (create-dir article-path)
      (spit (str article-path "/index.html")
            (app/article-detail (:slug article))))))


(defn -main [& args]
  (build-index)
  (build-blog))


; TODO: remove!
(comment
  (let [_ 1]
    ;(create-assets-dir)))
    ;(create-dist-dir)
    (build-index)))
   ;(app/index nil)))

(ns blog.build
  (:require [clojure.java.io :as io]
            [blog.pages :as pages]
            [blog.articles :as articles]
            [blog.app :as app]))


(defn create-dist-dir
  []
  (.mkdir (java.io.File. "dist")))


(defn build-index
  []
  (spit "dist/index.html" (app/index nil)))


(comment
  (let [_ 1]
    ;(create-assets-dir)))
    ;(create-dist-dir)
    (build-index)))
   ;(app/index nil)))

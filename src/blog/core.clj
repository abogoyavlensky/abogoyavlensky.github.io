(ns blog.core
  (:gen-class)
  (:require [clojure.string :as str]
            [rum.core :as rum]))


(defn -main
  [& _]
  ; TODO: think about moving running build static files from `build.clj`
  (println "Building static markups..."))

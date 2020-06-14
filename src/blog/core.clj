(ns blog.core
  (:gen-class)
  (:require [clojure.string :as str]
            [rum.core :as rum]))


(defn -main
  [& _]
  (println "Building static markups..."))  ; TODO: run building static html pages

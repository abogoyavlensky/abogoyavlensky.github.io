(ns blog.util.repl
  (:require [clojure.reflect :as reflect]
            [clojure.pprint :as pprint]))


(defn dir
  [instance]
  (->> instance
       (reflect/reflect)
       :members
       (pprint/print-table)))

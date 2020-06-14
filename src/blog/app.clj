(ns blog.app
  (:require [compojure.core :refer [defroutes GET]]
            [compojure.route :refer [not-found]]
            [ring.util.response :refer [response]]
            [com.stuartsierra.component :as component]
            [blog.server :refer [new-server]]))


(defn- home
  [_]
  ;(response "<h1>Hello!</h1>")
  (prn "TEST!!!!!!!!!")
  (response "<h1>Hello!</h1>"))


(defroutes routes
  (GET "/" [] home)
  (not-found "Page not found"))


(defn app
  [request]
  (routes request))


(defn new-system
  [app]
  (component/system-map
    :server (new-server app)))

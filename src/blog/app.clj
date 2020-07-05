(ns blog.app
  (:require [clojure.java.io :as io]
            [compojure.core :refer [defroutes GET]]
            [compojure.route :as route]
            [ring.util.response :refer [response]]
            [com.stuartsierra.component :as component]
            [blog.server :refer [new-server]]
            [blog.pages :as pages]
            [rum.core :as rum]))


(defn html-response
  [body]
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body body})


(defn- render-page
  [template]
  (-> template
    (rum/render-static-markup)
    (str "<!DOCTYPE html>\n")
    html-response))


(defn- index
  [_]
  (->> (pages/base "Andrey Bogoyavlensky's blog")
    (render-page)))


(defroutes routes
  (GET "/" [] index)
  (route/resources "/assets")
  (GET "/favicon.ico" _
    (-> "public/images/favicon.ico"
      io/resource
      io/input-stream
      response))
  (route/not-found "Page not found"))


(defn app
  []
  (fn [request]
    (routes request)))


(defn new-system
  [app]
  (component/system-map
    :server (new-server app)))

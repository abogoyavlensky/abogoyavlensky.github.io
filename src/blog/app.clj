(ns blog.app
  (:require [clojure.java.io :as io]
            [compojure.core :refer [defroutes GET]]
            [compojure.route :as route]
            [ring.util.response :refer [response]]
            [com.stuartsierra.component :as component]
            [rum.core :as rum]
            [blog.server :refer [new-server]]
            [blog.pages :as pages]
            [blog.articles :as articles]))


(defn- html-response
  [body]
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body body})


(defn- render-page
  [css-file-name title content]
  (->> content
    (pages/base css-file-name title)
    (rum/render-static-markup)
    (str "<!DOCTYPE html>\n")))


(defn index
  [_request css-file-name]
  (->> (articles/meta-data)
    (articles/articles-list-data)
    (pages/articles-list)
    (render-page css-file-name "Andrey Bogoyavlensky | Blog")))


(defn article-detail
  [slug css-file-name]
  (->> (articles/meta-data)
       (articles/articles-list-data)
       (articles/article-detail-data slug)
       (pages/article-detail)
       ; TODO: update with actual article's title
       (render-page css-file-name "Andrey Bogoyavlensky | Blog")))


(defroutes routes
  (GET "/" request (-> (index request nil)
                       (html-response)))
  (GET "/blog/:slug" [slug] (-> (article-detail slug nil)
                                (html-response)))
  (route/resources "/assets")
  (route/not-found "Page not found"))


(defn app
  []
  (fn [request]
    (routes request)))


(defn new-system
  [app]
  (component/system-map
    :server (new-server app)))


; TODO: remove
(comment
  (let [site-data (articles/meta-data)
        title "Testing title"]
    (->> (pages/articles (articles/articles-list-data (articles/meta-data)))
      (pages/base title)
      (rum/render-static-markup)
      (str "<!DOCTYPE html>\n"))))

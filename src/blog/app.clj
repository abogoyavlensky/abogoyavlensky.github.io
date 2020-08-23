(ns blog.app
  (:require [clojure.java.io :as io]
            [compojure.core :refer [defroutes GET]]
            [compojure.route :as route]
            [com.stuartsierra.component :as component]
            [rum.core :as rum]
            [blog.server :refer [new-server]]
            [blog.pages :as pages]
            [blog.articles :as articles]
            [clj-rss.core :as rss]))


(defn- html-response
  [body]
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body body})


(defn- xml-response
  [body]
  {:status 200
   :headers {"Content-Type" "text/xml; charset=utf-8"}
   :body body})


(defn- plain-response
  [body]
  {:status 200
   :headers {"Content-Type" "text/plain"}
   :body body})


(defn- render-page
  [current-page css-file-name title content]
  (->> content
    (pages/base current-page css-file-name title)
    (rum/render-static-markup)
    (str "<!DOCTYPE html>\n")))


(defn index
  [_request css-file-name]
  (->> (articles/meta-data)
    (articles/articles-list-data)
    (pages/articles-list)
    (render-page pages/PAGE-BLOG css-file-name "Andrey Bogoyavlensky | Blog")))


(defn article-detail
  [slug css-file-name]
  (->> (articles/meta-data)
       (articles/articles-list-data)
       (articles/article-detail-data slug)
       (pages/article-detail)
       ; TODO: update with actual article's title
       (render-page pages/PAGE-BLOG css-file-name "Andrey Bogoyavlensky | Blog")))


(defn projects
  [_request css-file-name]
  (->> (pages/projects)
       (render-page pages/PAGE-PROJECTS css-file-name "Andrey Bogoyavlensky | Projects")))


(defn about
  [_request css-file-name]
  (->> (pages/about)
       (render-page pages/PAGE-ABOUT css-file-name "Andrey Bogoyavlensky | About")))


(defn not-found
  [_request css-file-name]
  (->> (pages/page-not-found)
       (render-page nil css-file-name "Andrey Bogoyavlensky | Page not found")))


(defn- article->feed-item
  [base-url article]
  (let [link (str base-url "/blog/" (:slug article))]
    {:title (:title article)
     :link link
     :guid link
     :author "Andrey Bogoyavlensky"
     :pubDate (:date article)}))


(defn feed
  [_request]
  (let [base-url "https://bogoyavlensky.com"
        channel {:title "Blog of Andrey Bogoyavlensky"
                 :link (str base-url "/")
                 :description "Notes mostly about programming"}]
    (->> (articles/meta-data)
         (articles/articles-list-data)
         (map #(article->feed-item base-url %))
         (rss/channel-xml channel))))


(defroutes routes
  (GET "/" request (-> (index request nil)
                       (html-response)))
  (GET "/blog/:slug" [slug] (-> (article-detail slug nil)
                                (html-response)))
  (GET "/projects" request (-> (projects request nil)
                               (html-response)))
  (GET "/about" request (-> (about request nil)
                            (html-response)))
  (GET "/feed.xml" request (-> (feed request)
                               (xml-response)))
  (route/resources "/assets")
  (GET "/robots.txt" _request (-> (io/resource "public/robots.txt")
                                  (slurp)
                                  (plain-response)))
  (route/not-found (-> (not-found nil nil)
                       (html-response))))


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

(ns blog.app
  (:require [clojure.java.io :as io]
            [clojure.xml :as xml]
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
  [current-page css-file-name html-meta content]
  (->> content
    (pages/base current-page css-file-name html-meta)
    (rum/render-static-markup)
    (str "<!DOCTYPE html>\n")))


(defn- base-html-meta
  [title path]
  {:title title
   :description "Blog of Andrey Bogoyavlensky mostly about programming"
   :keywords ["blog" "writing" "programming" "development" "software" "clojure"
              "clj" "cljs" "clojurescript" "python"]
   :canonical (str "https://bogoyavlensky.com/" path)
   :og-type :website})


(defn index
  [_request css-file-name]
  (->> (articles/meta-data)
    (articles/articles-list-data)
    (pages/articles-list)
    (render-page pages/PAGE-BLOG css-file-name (base-html-meta "Blog" nil))))


(defn- article-link
  [base-url article]
  (str base-url "/blog/" (:slug article) "/"))


(defn- article->html-meta
  [article]
  (-> article
    (select-keys [:title :description :keywords])
    (assoc :canonical (article-link "https://bogoyavlensky.com" article)
      :published (:date article)
      :og-type :article)))


(defn article-detail
  [slug css-file-name]
  (let [article (->> (articles/meta-data)
                  (articles/articles-list-data)
                  (articles/article-detail-data slug))]
    (->> article
      (pages/article-detail)
      (render-page pages/PAGE-BLOG css-file-name (article->html-meta article)))))


(defn projects
  [_request css-file-name]
  (->> (pages/projects)
    (render-page
      pages/PAGE-PROJECTS
      css-file-name
      (base-html-meta "Projects" "projects/"))))


(defn not-found
  [_request css-file-name]
  (->> (pages/page-not-found)
    (render-page nil css-file-name (base-html-meta "Page not found" nil))))


(defn- article->feed-item
  [base-url article]
  (let [link (article-link base-url article)]
    {:title (:title article)
     :link link
     :guid link
     :author "Andrey Bogoyavlensky"
     :pubDate (:date article)
     :description (:description article)}))


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


(defn sitemap-item
  [url]
  {:tag :url
   :content [{:tag :loc
              :content [url]}]})


(defn sitemap
  [_request]
  (let [base-url "https://bogoyavlensky.com"
        links (->>
                (articles/meta-data)
                (articles/articles-list-data)
                (map #(article-link base-url %))
                (cons (str base-url "/")))]
    (with-out-str
      (xml/emit
        {:tag :urlset
         :attrs {:xmlns "http://www.sitemaps.org/schemas/sitemap/0.9"}
         :content (map sitemap-item links)}))))


(defroutes routes
  (GET "/" request (-> (index request nil)
                     (html-response)))
  (GET "/blog/:slug" [slug] (-> (article-detail slug nil)
                              (html-response)))
  (GET "/projects" request (-> (projects request nil)
                             (html-response)))
  (GET "/feed.xml" request (-> (feed request)
                             (xml-response)))
  (GET "/robots.txt" _request (-> (io/resource "public/robots.txt")
                                (slurp)
                                (plain-response)))
  (GET "/sitemap.xml" request (-> (sitemap request)
                                (str)
                                (xml-response)))
  (route/resources "/assets")
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

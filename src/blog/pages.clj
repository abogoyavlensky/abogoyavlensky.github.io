(ns blog.pages
  (:require [rum.core :as rum]))


(rum/defc base
  [title]
  [:html
   [:head
    [:meta {:charset "UTF-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
    [:link {:rel "stylesheet" :href "/assets/css/style.css" :type "text/css"}]
    [:link {:rel "icon" :href "/favicon.ico"}]
    [:title title]]
   [:body
    [:div "Content."
     [:h1 "Some title!"]]]])

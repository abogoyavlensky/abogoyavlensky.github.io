(ns blog.pages
  (:require [rum.core :as rum]))


(rum/defc menu-item < rum/reactive
  [title url]
  (let [origin-classes ["text-xl" "font-mono" "ml-6" "text-gray-900"]]
    [:a
     {:href url
      :class origin-classes}
     title]))


(rum/defc menu < rum/static
  []
  [:div
   {:class ["max-w-xl" "mx-auto" "flex" "justify-between" "items-center" "h-24"]}
   [:div
    [:a
     {:href "/"
      :class ["text-2xl" "font-mono" "font-semibold" "text-gray-900"]}
     "bogoyavlensky.com"]]
   [:div
    {:class ["flex" "flex-row"]}
    (map #(apply menu-item %)
         [["blog" "/"]
          ["projects" "/projects"]
          ["about" "/about"]])]])


(rum/defc base
  [title content]
  [:html
   [:head
    [:meta {:charset "UTF-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
    [:link {:rel "stylesheet" :href "/assets/css/style.css" :type "text/css"}]
    [:link {:rel "icon" :href "/favicon.ico"}]
    [:title title]]
   [:body
    (menu)
    content]])


(rum/defc article-list-item
  [item]
  [:div
   {:class ["mb-5"]}
   [:h2
    {:class ["text-3xl text-gray-900 leading-tight font-sans"]}
    [:a
     {:href (str "/blog/" (:slug item))}
     (:title item)]]
   [:p {:class ["text-base text-gray-600 leading-normal font-sans"]} (:date item)]])


(rum/defc articles
  [articles-data]
  [:div
   {:class ["max-w-xl" "mx-auto" "text-left" "py-12"]}
   [:div (map article-list-item articles-data)]])

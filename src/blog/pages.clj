(ns blog.pages
  (:require [rum.core :as rum]))


(def ^:private MAX-WIDTH "max-w-2xl")


(rum/defc menu-item
  [title url]
  (let [origin-classes ["text-xl" "font-mono" "ml-6" "text-gray-900"]]
    [:a
     {:href url
      :class origin-classes}
     title]))


(rum/defc menu
  []
  [:div
   {:class [MAX-WIDTH "mx-auto"]}
   [:header
    {:class ["flex" "justify-between" "items-center" "h-24"]}
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
           ["about" "/about"]])]]])


(rum/defc article-list-item
  [item]
  [:div
   {:class ["mb-5"]}
   [:h2
    {:class ["text-2xl text-gray-900 leading-tight font-sans"]}
    [:a
     {:href (str "/blog/" (:slug item))}
     (:title item)]]
   [:p
    {:class ["text-base text-gray-600 leading-normal font-sans"]}
    (:date item)]])


(rum/defc articles-list
  [articles-data]
  [:div
   (map article-list-item articles-data)])


(rum/defc article-detail
  [article]
  [:article
   [:div
    [:h1
     {:class ["text-4xl"
              "leading-9"
              "font-extrabold"
              "text-gray-900"
              "tracking-tight"]}
     (:title article)]
    [:span
     {:class ["mt-0" "mb-4" "text-gray-600"]}
     (:date article)]]
   [:div {:class ["prose" "prose-lg" "mt-10" "max-w-none"]
          :dangerouslySetInnerHTML {:__html (:text article)}}]])


(rum/defc base
  [title content]
  [:html
   [:head
    [:meta {:charset "UTF-8"}
     [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]]
    [:link {:rel "stylesheet" :href "/assets/css/output.css" :type "text/css"}]
    ; TODO: remove and update on my own theme
    [:link {:rel "stylesheet" :href "/assets/css/dark.min.css" :type "text/css"}]
    ;[:link {:rel "stylesheet" :href "/assets/css/atom-one-dark.min.css" :type "text/css"}]
    ;[:link {:rel "stylesheet" :href "/assets/css/tomorrow-night-blue.min.css" :type "text/css"}]
    [:link {:rel "icon" :href "/assets/images/favicon.ico"}]
    [:title title]]
   [:body
    {:class ["overflow-y-scroll"]}
    (menu)
    [:div
     {:class [MAX-WIDTH "mx-auto" "mt-12"]}
     content]
    [:script {:src "/assets/js/highlight.pack.js"}]
    [:script "hljs.initHighlightingOnLoad();"]]])

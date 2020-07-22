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
    {:class ["text-sm text-gray-600 leading-normal font-sans"]}
    (:date item)]])


(rum/defc articles-list
  [articles-data]
  [:div
   (map article-list-item articles-data)])


(def h1-style
  ["text-4xl"
   "leading-9"
   "font-bold"
   "text-gray-900"
   "tracking-tight"])


(def h2-style
  ["text-3xl"
   "leading-9"
   "text-gray-900"
   "tracking-tight"])


(rum/defc article-detail
  [article]
  [:article
   [:div
    [:h1
     {:class (concat h1-style ["mb-1"])}
     (:title article)]
    [:span
     {:class ["mt-0" "mb-4" "text-gray-600"]}
     (:date article)]]
   [:div {:class ["prose" "prose-lg" "mt-10" "max-w-none"]
          :dangerouslySetInnerHTML {:__html (:text article)}}]])


(rum/defc page-not-found
  []
  [:div
   [:h1
    {:class (concat h2-style ["mb-10"])}
    "Page not found"]
   [:a
    {:href "/"
     :class ["bg-transparent" "hover:bg-indigo-500" "text-indigo-700" "font-semibold"
             "hover:text-white" "py-2" "px-4" "border" "border-indigo-500" "hover:border-transparent"
             "rounded"]}
    ">> back to the site"]])


(rum/defc base
  [css-file-name title content]
  (let [css-file (if (some? css-file-name)
                   css-file-name
                   "output.css")]
    [:html
     [:head
      [:meta {:charset "UTF-8"}
       [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]]
      [:link {:rel "stylesheet" :href (str "/assets/css/" css-file) :type "text/css"}]
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
      [:script "hljs.initHighlightingOnLoad();"]]]))

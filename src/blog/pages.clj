(ns blog.pages
  (:require [rum.core :as rum]))


(def ^:private MAX-WIDTH "max-w-2xl")
(def PAGE-BLOG :blog)
(def PAGE-PROJECTS :projects)
(def PAGE-ABOUT :about)


(rum/defc menu-item
  [page-name url current-page]
  (let [base-style ["text-xl" "font-mono" "ml-6" "text-gray-900" "border-b-4" "border-transparent"]
        active-style ["border-indigo-500"]]
    [:a
     {:href url
      :class (if (= current-page page-name)
               (concat base-style active-style)
               base-style)}
     (name page-name)]))


(rum/defc menu
  [current-page]
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
     {:class ["flex" "flex-row" "mt-2"]}
     (map #(apply menu-item %)
          [[PAGE-BLOG "/" current-page]
           [PAGE-PROJECTS "/projects" current-page]
           [PAGE-ABOUT "/about" current-page]])]]])


(rum/defc article-list-item
  [item]
  [:div
   {:class ["mb-5"]}
   [:h2
    {:class ["text-2xl" "text-gray-900" "leading-tight" "font-sans" "hover:underline"]}
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
  ["text-2xl"
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


(rum/defc projects
  []
  [:div
   [:h2
    {:class (concat h2-style ["mb-10"])}
    "Projects"]
   [:h2
    {:class (concat h2-style ["mb-10"])}
    "Libs"]
   [:h2
    {:class (concat h2-style ["mb-10"])}
    "Contributions"]])


(rum/defc about
  []
  [:div
   [:p
    {:class (concat h2-style ["mb-10"])}
    "Hi! My name is Andrey Bogoyavlensky. And it's my personal blog where
    you could find articles and notes mostly about programming.
    I'm a software engineer with about eight years of production development experience of web systems on a different scale.
    My knowledge mostly relates to Python/Django but now my main language is Clojure.
    Which I've been using about a year on a daily basis.
    Hope you will find something interesting for you on these pages.
    Best regards!"]])


(rum/defc base
  [current-page css-file-name title content]
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
      (menu current-page)
      [:div
       {:class [MAX-WIDTH "mx-auto" "mt-12"]}
       content]
      [:script {:src "/assets/js/highlight.pack.js"}]
      [:script "hljs.initHighlightingOnLoad();"]]]))

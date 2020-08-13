(ns blog.pages
  (:require [rum.core :as rum]
            [blog.icons :as icons]))


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
     {:class ["flex" "flex-row" "mt-3"]}
     (map #(apply menu-item %)
          [[PAGE-BLOG "/" current-page]
           ; TODO: uncomment!
           ;[PAGE-PROJECTS "/projects" current-page]
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
    (:date-str item)]])


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
     (:date-str article)]]
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
    {:class ["text-xl" "text-gray-800" "mb-10"]}
    "Hi! I'm Andrey Bogoyavlensky. And it's my personal blog where
    you could find articles and notes mostly about programming.
    I'm a software engineer with about eight years of production development experience of web systems on a different scale.
    Originally, I used to use Python/Django for backend but now my main tech stack based on Clojure.
    Hope you will find something interesting for you on these pages.
    Best regards!"]])


(rum/defc icon-link
  [icon-name link]
  [:a
   {:class ["p-2" "hover:bg-gray-200" "rounded-full" "ml-2"]
    :href link
    :target "_blank"}
   (icons/icon icon-name)])


(rum/defc footer
  []
  [:footer
   [:div
    {:class [MAX-WIDTH "mx-auto" "flex" "justify-between" "items-center"
             "h-24" "border-t" "border-gray-300" "mt-32"]}
    [:div
     {:class []}
     "© Published since 2020"]
    [:div
     {:class ["flex" "flex-row"]}
     (map #(apply icon-link %)
          [[icons/GITHUB-ICON-NAME "https://github.com/abogoyavlensky"]
           [icons/TWITTER-ICON-NAME "https://twitter.com/abogoyavlensky"]
           ; TODO: add link to RSS feed of current blog
           [icons/RSS-ICON-NAME "/feed.xml"]])]]])


(rum/defc base
  [current-page css-file-name title content]
  (let [css-file (if (some? css-file-name)
                   css-file-name
                   "output.css")]
    [:html
     {:class ["h-full"]}
     [:head
      [:meta {:charset "UTF-8"}
       [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]]
      [:link {:rel "stylesheet" :href (str "/assets/css/" css-file) :type "text/css"}]
      [:link {:rel "stylesheet" :href "/assets/css/dark.min.css" :type "text/css"}]
      [:link {:rel "icon" :href "/assets/images/favicon.ico"}]
      [:title title]]
     [:body
      {:class ["overflow-y-scroll" "flex" "flex-col" "h-full" "bg-white"]}
      [:div
       {:class ["flex-1"]}
       (menu current-page)
       [:div
        {:class [MAX-WIDTH "mx-auto" "mt-12"]}
        content]]
      (footer)
      [:script {:src "/assets/js/highlight.pack.js"}]
      [:script "hljs.initHighlightingOnLoad();"]]]))

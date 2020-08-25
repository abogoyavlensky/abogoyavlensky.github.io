(ns blog.pages
  (:require [clojure.string :as str]
            [rum.core :as rum]
            [blog.icons :as icons]
            [tick.alpha.api :as t]))


(def ^:private MAX-WIDTH "max-w-3xl")
(def PAGE-BLOG :blog)
(def PAGE-PROJECTS :projects)
(def PAGE-ABOUT :about)


(rum/defc menu-item
  [page-name url current-page]
  (let [base-style ["text-xl" "font-mono" "ml-6" "text-gray-900" "border-b-4" "border-transparent"]
        active-style ["border-indigo-500"]
        passive-style ["hover:border-gray-300"]]
    [:a
     {:href url
      :class (if (= current-page page-name)
               (concat base-style active-style)
               (concat base-style passive-style))}
     (name page-name)]))


(rum/defc menu
  [current-page]
  [:div
   {:class [MAX-WIDTH "mx-auto"]}
   [:header
    {:class ["sm:flex" "sm:justify-between" "items-center" "h-24"]}
    [:div
     {:class ["flex" "justify-start" "mt-4" "sm:mt-0"]}
     [:a
      {:href "/"
       :class ["text-2xl" "font-mono" "font-bold" "text-gray-900"]}
      "bogoyavlensky.com"]]
    [:div
     {:class ["flex" "justify-start" "sm:flex-row" "mt-3" "-ml-6" "sm:ml-0"]}
     (map #(apply menu-item %)
       [[PAGE-BLOG "/" current-page]
        [PAGE-PROJECTS "/projects" current-page]
        [PAGE-ABOUT "/about" current-page]])]]])


(rum/defc article-list-item
  [item]
  [:div
   {:class ["mb-3" "sm:mb-5"]}
   [:h2
    {:class ["text-xl" "sm:text-2xl" "text-gray-900" "leading-tight" "font-sans" "hover:underline"]}
    [:a
     {:href (str "/blog/" (:slug item))}
     (:title item)]]
   [:p
    {:class ["text-xs" "sm:text-sm text-gray-600 leading-normal font-sans"]}
    (:date-str item)]])


(rum/defc articles-list
  [articles-data]
  [:div
   (map article-list-item articles-data)])


(def h1-style
  ["text-3xl"
   "sm:text-4xl"
   "leading-6"
   "sm:leading-9"
   "font-bold"
   "text-gray-900"
   "tracking-tight"])


(def h2-style
  ["text-xl"
   "sm:text-2xl"
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
     {:class ["text-sm" "sm:text-base" "mt-0" "mb-4" "text-gray-600"]}
     (:date-str article)]]
   [:div {:class ["prose" "sm:prose-xl" "mt-6" "sm:mt-10" "max-w-none"]
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


(rum/defc card-btn-ext
  [text url color hover-color]
  [:a
   {:class ["px-3" "py-2" "ml-2" color "text-white" "text-xs" "font-bold"
            "uppercase" "rounded" hover-color]
    :href url
    :target "_blank"}
   [:span
    {:class ["inline-block"]}
    text]
   [:div
    {:class ["inline-block" "ml-1" "-mr-1" "align-top"]}
    (icons/icon-sm :external-link icons/ICON-COLOR-WHITE)]])


(rum/defc project-card
  [project]
  (let [image-base "/assets/images"]
    [:div
     {:class ["max-w-sm" "shadow-lg" "rounded-lg" "overflow-hidden"]}
     (when (some? (:image project))
       [:img
        {:class ["w-full"]
         :src (str image-base (:image project))}])
     [:div
      {:class ["p-4"]}
      [:div
       {:class ["flex" "justify-between" "sm:justify-between"]}
       [:h1
        {:class ["text-gray-900" "font-bold" "text-2xl"]}
        (:title project)]
       (when (some? (:logo project))
         [:img
          {:class ["w-10" "h-10" "object-cover" "rounded-full" "border-2" "border-indigo-500"]
           :src (str image-base (:logo project))}])]
      [:p
       {:class ["mt-2" "text-gray-600" "text-sm"]}
       (:description project)]
      [:p
       {:class ["mt-2" "mb-2" "text-gray-800" "text-sm"]}
       (str/join #", " (:stack project))]
      [:div
       {:class ["flex" "justify-end"]}
       (when (some? (:url project))
         (card-btn-ext "Link" (:url project) "bg-green-700" "hover:bg-green-800"))
       (card-btn-ext "Source" (:source project) "bg-gray-700" "hover:bg-gray-900")]]]))


(rum/defc projects-section
  [title card-fn items]
  [:div
   [:h2
    {:class (concat h2-style ["mb-6" "mt-16" "font-mono"])}
    title]
   [:div
    {:class ["grid" "grid-cols-1" "sm:grid-cols-2" "gap-4" "sm:gap-5"]}
    (map card-fn items)]])


(rum/defc projects
  []
  [:div
   (projects-section
     "Projects"
     project-card
     [{:title "Blog"
       :description "Statically generated blog pages by custom engine."
       :url "https://bogoyavlensky.com/"
       :source "https://github.com/abogoyavlensky/abogoyavlensky.github.io"
       :image "/projects/blog_preview_850.png"
       :stack ["Clojure" "Rum" "Tailwind CSS"]}])
   (projects-section
     "Libraries"
     project-card
     [{:title "drf-common-exceptions"
       :description "Common exceptions handler for Django REST framework"
       :source "https://github.com/abogoyavlensky/drf-common-exceptions"
       :stack ["Python" "Django"]}
      {:title "drf-action-permissions"
       :description "Flexible action level permissions for Django REST framework"
       :source "https://github.com/abogoyavlensky/drf-action-permissions"
       :stack ["Python" "Django"]}
      {:title "cookiecutter-django-api"
       :description " Full featured Django API boilerplate"
       :source "https://github.com/abogoyavlensky/cookiecutter-django-api"
       :stack ["Python" "Django" "Cookiecutter"]}])
   (projects-section
     "Contributions"
     project-card
     [{:title "clojureVSCode"
       :description "Clojure/ClojureScript support for Visual Studio Code"
       :source "https://github.com/avli/clojureVSCode"
       :url "https://marketplace.visualstudio.com/items?itemName=avli.clojure"
       :stack ["TypeScript" "VS Code" "Clojure"]}
      {:title "clj-kondo"
       :description "A linter for Clojure code"
       :source "https://github.com/borkdude/clj-kondo"
       :stack ["Clojure"]}])])


(rum/defc about
  []
  [:div
   [:img
    {:class ["w-2/6" "rounded-full" "float-left" "mr-4" "mb-4"]
     :src "/assets/images/my_photo_850.jpg"
     :alt "My photo"}]
   [:p
    {:class ["text-base" "sm:text-xl" "text-gray-800" "mb-10" "leading-relaxed"]}
    "Hi there! My name is Andrey Bogoyavlensky.
    I'm a software engineer with a production development experience of web systems on a different scale.
    Originally, I used to use Python/Django but now my main tech stack is based on Clojure.
    It's my personal blog where you could find articles and notes mostly about programming.
    Hope you enjoy the content.
    Best regards!"]])


(rum/defc icon-link
  [icon-name link]
  [:a
   {:class ["p-2" "hover:bg-gray-200" "rounded-full" "ml-2"]
    :href link
    :target "_blank"}
   (icons/icon-md icon-name)])


(rum/defc footer
  []
  [:footer
   [:div
    {:class [MAX-WIDTH "mx-auto" "flex" "justify-between" "items-center"
             "h-24" "border-t" "border-gray-300" "mt-32"]}
    [:div
     {:class ["text-sm" "sm:text-base"]}
     "© Since 2020 | Andrey Bogoyavlensky"]
    [:div
     {:class ["flex" "flex-row"]}
     (map #(apply icon-link %)
       [[:github "https://github.com/abogoyavlensky"]
        [:twitter "https://twitter.com/abogoyavlensky"]
        [:rss "/feed.xml"]])]]])


(defn- base-og-tags
  [html-meta]
  [[:meta {:property "og:site_name"
           :content "bogoyavlensky.com"}]
   [:meta {:property "og:description"
           :content (:description html-meta)}]
   [:meta {:property "og:title"
           :content (:title html-meta)}]
   [:meta {:property "og:url"
           :content (:canonical html-meta)}]
   [:meta {:property "og:type"
           :content (:og-type html-meta)}]])


(defmulti meta-og-tags :og-type)


(defmethod meta-og-tags :website
  [html-meta]
  (base-og-tags html-meta))


(defmethod meta-og-tags :article
  [html-meta]
  (concat (base-og-tags html-meta)
    [[:meta {:property "article:author"
             :content "Andrey Bogoyavlensky"}]
     (map (fn [tag]
            [:meta {:property "article:tag"
                    :content tag}])
       (take 3 (:keywords html-meta)))
     [:meta {:property "article:published_time"
             :content (t/instant (:published html-meta))}]]))


(rum/defc base
  [current-page css-file-name html-meta content]
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
      [:title (str (:title html-meta) " | Andrey Bogoyavlensky")]
      [:meta {:name :author
              :content "Andrey Bogoyavlensky"}]
      (when (some? (:description html-meta))
        [:meta {:name :description
                :content (:description html-meta)}])
      (when (some? (:keywords html-meta))
        [:meta {:name :keywords
                :content (str/join #", " (:keywords html-meta))}])
      (when (some? (:canonical html-meta))
        [:link {:rel "canonical"
                :href (:canonical html-meta)}])
      (when (some? (:og-type html-meta))
        (seq (meta-og-tags html-meta)))]
     [:body
      {:class ["overflow-y-scroll" "flex" "flex-col" "h-full" "bg-white" "mx-5" "sm:mx-0"]}
      [:div
       {:class ["flex-1"]}
       (menu current-page)
       [:div
        {:class [MAX-WIDTH "mx-auto" "mt-12"]}
        content]]
      (footer)
      [:script {:src "/assets/js/highlight.pack.js"}]
      [:script "hljs.initHighlightingOnLoad();"]]]))

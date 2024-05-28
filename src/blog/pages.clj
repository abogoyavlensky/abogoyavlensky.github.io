(ns blog.pages
  (:require [clojure.string :as str]
            [rum.core :as rum]
            [blog.icons :as icons]
            [tick.alpha.api :as t]))


(def ^:private MAX-WIDTH "max-w-3xl")
(def PAGE-BLOG :blog)
(def PAGE-PROJECTS :projects)


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
        [PAGE-PROJECTS "/projects" current-page]])]]])


(rum/defc about
  []
  [:div
   {:class ["shadow-lg" "rounded-lg" "p-4" "pb-6"]}
   [:img
    {:class ["w-24" "sm:w-32" "rounded-full" "float-left" "mr-4" "mb-2"]
     :src "/assets/images/my_photo_850.jpg"
     :alt "My photo"}]
   [:p
    {:class ["text-base" "sm:text-lg" "text-gray-800" "leading-relaxed"]}
    (str "Welcome to my blog! "
      "I'm Andrey Bogoyavlenskiy, a software engineer with over 10 years in the field."
      " My tech journey has evolved from Python to a focus on Clojure."
      " Here, I share my programming insights and experiences. Enjoy exploring!")]])


(rum/defc article-list-item
  [item]
  [:div
   {:class ["mb-3" "sm:mb-5"]}
   [:h2
    {:class ["text-xl" "sm:text-2xl" "text-gray-900" "leading-tight" "font-sans" "underline"]}
    [:a
     {:href (str "/blog/" (:slug item))}
     (:title item)]]
   [:p
    {:class ["text-xs" "sm:text-sm text-gray-600 leading-normal font-sans"]}
    (:date-str item)]])


(rum/defc articles-list
  [articles-data]
  [:div
   (about)
   [:div
    {:class ["flex" "justify-center" "mt-16" "sm:mt-24"]}
    [:div
     {:class ["border-t" "border-gray-300" "w-24" "sm:w-40"]}]]
   [:div
    {:class ["mt-6" "sm:mt-10"]}
    (map article-list-item articles-data)]])


(def h1-style
  ["text-4xl"
   "md:text-5xl"
   "text-gray-900"
   "tracking-normal"
   "leading-snug"])


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
   [:div {:class ["prose" "sm:prose-lg" "md:prose-xl" "mt-6" "sm:mt-10" "max-w-none"]
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
       {:class ["flex" "justify-between"]}
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
    {:class (concat h2-style ["mb-4" "font-mono"])}
    title]
   [:div
    {:class ["grid" "grid-cols-1" "sm:grid-cols-2" "gap-4" "sm:gap-5" "mb-8" "md:mb-16"]}
    (map card-fn items)]])


(rum/defc contribution-card
  [project]
  [:div
   {:class ["max-w-full" "shadow-md" "rounded-lg" "overflow-hidden"]}
   [:div
    {:class ["p-4"]}
    [:div
     [:div
      [:h1
       {:class ["text-gray-900" "font-bold" "text-xl" "mr-4"]}
       [:a
        {:class ["underline"]
         :href (:source project)
         :target "_blank"}
        (:title project)]]]
     [:p
      {:class ["mt-2" "text-gray-600" "text-sm"]}
      (:description project)]]]])


(rum/defc contributions-section
  [title card-fn items]
  [:div
   [:h2
    {:class (concat h2-style ["mb-4" "font-mono"])}
    title]
   [:div
    {:class ["mb-8" "md:mb-16"]}
    (map card-fn items)]])


(rum/defc projects
  []
  [:div
   {:class ["grid" "grid-cols-1" "gap-20"]}
   (projects-section
     "Projects"
     project-card
     [{:title "Blog"
       :description "Statically generated blog pages by custom engine."
       :url "https://bogoyavlensky.com/"
       :source "https://github.com/abogoyavlensky/abogoyavlensky.github.io"
       :image "/projects/blog_preview_home.png"
       :stack ["Clojure" "Rum" "Tailwind CSS"]}])
   (projects-section
     "Libraries"
     project-card
     [{:title "automigrate"
       :description "Database auto-migration tool for Clojure"
       :source "https://github.com/abogoyavlensky/automigrate"
       :stack ["Clojure"]}
      {:title "eftest-coverage"
       :description "Test coverage for Eftest Clojure test runner using Cloverage"
       :source "https://github.com/abogoyavlensky/eftest-coverage"
       :stack ["Clojure"]}
      {:title "drf-common-exceptions"
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
   (contributions-section
     "Contributions"
     contribution-card
     [{:title "avli/clojureVSCode"
       :description "Clojure/ClojureScript support for Visual Studio Code"
       :source "https://github.com/avli/clojureVSCode/commits?author=abogoyavlensky"
       :url "https://marketplace.visualstudio.com/items?itemName=avli.clojure"
       :stack ["TypeScript" "VS Code" "Clojure"]}
      {:title "clj-kondo/clj-kondo"
       :description "A linter for Clojure code"
       :source "https://github.com/borkdude/clj-kondo/commits?author=abogoyavlensky"
       :stack ["Clojure"]}
      {:title "cloverage/cloverage"
       :description "Clojure test coverage tool"
       :source "https://github.com/cloverage/cloverage/commits?author=abogoyavlensky"
       :stack ["Clojure"]}
      {:title "com.github.liquidz/antq"
       :description "Point out your Clojure outdated dependencies."
       :source "https://github.com/liquidz/antq/commits?author=abogoyavlensky"
       :stack ["Clojure"]}
      {:title "practicalli/clojure-deps-edn"
       :description "A collection of useful configuration and aliases for deps.edn based projects"
       :source "https://github.com/practicalli/clojure-deps-edn/commits?author=abogoyavlensky"
       :stack ["Clojure"]}
      {:title "asdf-vm/asdf-plugins"
       :description "Convenience shortname repository for asdf community plugins"
       :source "https://github.com/asdf-vm/asdf-plugins/commits?author=abogoyavlensky"
       :stack ["Shell" "Clojure"]}])])


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
     "© Andrey Bogoyavlenskiy | Since 2020"]
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
           :content (:og-type html-meta)}]
   [:meta {:property "og:image"
           :content (:og-image html-meta)}]])


(defmulti meta-og-tags :og-type)


(defmethod meta-og-tags :website
  [html-meta]
  (base-og-tags html-meta))


(defmethod meta-og-tags :article
  [html-meta]
  (concat (base-og-tags html-meta)
    [[:meta {:property "article:author"
             :content "Andrey Bogoyavlenskiy"}]
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
      [:script {:defer "true"
                :src "https://cloud.umami.is/script.js"
                :data-website-id "98a7d7bf-8c50-4645-ac77-6bf940d8d766"}]
      [:title (str (:title html-meta) " | Andrey Bogoyavlenskiy")]
      [:meta {:name :author
              :content "Andrey Bogoyavlenskiy"}]
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
      {:class ["overflow-y-scroll" "flex" "flex-col" "h-full" "bg-white" "mx-5" "md:mx-0"]}
      [:div
       {:class ["flex-1"]}
       (menu current-page)
       [:div
        {:class [MAX-WIDTH "mx-auto" "mt-10" "md:mt-12"]}
        content]]
      (footer)
      [:script {:src "/assets/js/highlight.pack.js"}]
      [:script "hljs.initHighlightingOnLoad();"]]]))

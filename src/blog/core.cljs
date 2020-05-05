(ns ^:figwheel-hooks blog.core
  (:require [cljs.reader :as reader]
            [clojure.string :as str]
            [rum.core :as rum]
            [ajax.core :as ajax]
            [pushy.core :as pushy]
            [bidi.bidi :as bidi]))


(defonce app-state (atom {:router nil
                          :meta nil}))


(def routes
  ["/" [["" :home]
        [["articles/" :slug] :article-detail]
        [["projects/"] :projects]
        [["blog/"] :blog]]])


(def api-routes
  ["/data/" {"meta.edn" :meta-data
             ["articles/" :slug ".md"] :article-detail}])


(defn api-url
  [url-key params]
  (let [url-fn (partial bidi/path-for api-routes url-key)
        params* (some-> params
                  (seq)
                  (flatten))]
    (if params*
      (apply url-fn params*)
      (url-fn))))


(defn fetch
  [url handler]
  (-> (ajax/GET url {:handler handler})))


(defn ajax-mixin
  [url-key state-key]
  {:will-mount
   (fn [state]
      (let [*data (atom nil)
            comp (:rum/react-component state)
            params (-> state :rum/args first)
            url (api-url url-key params)
            update-data! (fn [data]
                           (reset! *data
                             (if (str/ends-with? url ".edn")
                               (reader/read-string data)
                               data))
                           (rum/request-render comp))]
        (fetch url update-data!)
        (assoc state state-key *data)))})


(rum/defc article-list-item
  [item]
  [:div
   {:class ["mb-5"]}
   [:h2
    {:class ["text-3xl text-gray-900 leading-tight font-sans"]}
    [:a
     {:href (bidi/path-for routes :article-detail :slug (:slug item))}
     (:title item)]]
   [:p {:class ["text-base text-gray-600 leading-normal font-sans"]} (:date item)]])


(rum/defcs articles < (ajax-mixin :meta-data ::meta-data)
  [state]
  [:div
   {:class ["max-w-xl" "mx-auto" "text-left" "py-12"]}
   (if-let [articles-data (:articles @(::meta-data state))]
     [:div (map article-list-item articles-data)]
     [:div "Loading..."])])


(rum/defcs article
  < (ajax-mixin :meta-data ::meta-data)
  < (ajax-mixin :article-detail ::article)
  [state params]
  (let [slug (:slug params)
        article-data (->> @(::meta-data state)
                       :articles
                       (filter #(= (:slug %) slug))
                       (first))
        article-md @(::article state)]
    (if (and article-data article-md)
      [:div
       [:h2 (:title article-data)]
       [:div {:dangerouslySetInnerHTML {:__html article-md}}]
       [:span (:date article-data)]]
      [:div "Loading..."])))


(rum/defc page-not-found < rum/static
  []
  [:h2 "Page not found!"
   [:br]
   [:a {:href (bidi/path-for routes :home)} "Main page"]])


(def underlined-menu
  ["border-b-4" "border-green-500"])


(rum/defc menu-item
  < rum/reactive
  < {:key-fn (fn [_ title _]
               title)}
  [handlers title url]
  (let [origin-classes ["text-xl" "font-mono" "ml-6" "text-gray-900"]
        current-handler (get-in (rum/react app-state) [:router :handler])]
    ;updated-class (if (contains? handlers current-handler))
    ;updated-class (if false
    ;                (concat origin-classes underlined-menu))]
    [:a
     {:href url
      :class origin-classes
      :key url}
     title]))


(rum/defc menu < rum/static
  []
  [:div
   {:class ["max-w-xl" "mx-auto" "flex" "justify-between" "items-center" "h-24"]}
   [:div
    [:a
     {:href (bidi/path-for routes :home)
      :class ["text-2xl" "font-mono" "font-semibold" "text-gray-900"]}
     "bogoyavlensky.com"]]
   [:div
    {:class ["flex" "flex-row"]}
    (map #(apply menu-item %)
      [[#{:home :article-detail} "blog" (bidi/path-for routes :home)]
       [#{:projects} "projects" (bidi/path-for routes :projects)]
       [#{:about} "about" (bidi/path-for routes :about)]])]])


(rum/defc root < rum/reactive
  [state]
  [:div
   (menu)
   (let [router (:router (rum/react state))]
     (prn router)
     (case (:handler router)
       :home (articles)
       :article-detail (article (:route-params router))
       (page-not-found)))])


(defn on-set-page!
  [match]
  (swap! app-state assoc :router match))


(def history
  (pushy/pushy
    on-set-page!
    (partial bidi/match-route routes)))


(pushy/start! history)


(defn render
  []
  (prn "RENDER")
  (rum/mount (root app-state) (. js/document (getElementById "app"))))


;; specify reload hook with ^;after-load metadata
(defn ^:after-load on-reload []
  (render)
  ;; force rerendering depending on application state
  (swap! app-state update-in [:__figwheel_counter] inc))




(comment
  (let [params {:slug 123}]
    ;(apply demo (flatten (into [] params)))
    ;(apply bidi/path-for routes :article-detail (-> params seq flatten))
    ;(apply bidi/path-for routes :article-detail nil)
    (api-url :article-detail params)
    (api-url :article-detail {:id "awesome"})
    (fetch "/api/articles/awesome" (fn [x] (js/console.log (:title x))))))


(comment
  (prn "100")
  (+ 1 1)
  (js/console.log "TEST!!!!!"))

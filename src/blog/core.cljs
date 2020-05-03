(ns ^:figwheel-hooks blog.core
  (:require
   [goog.dom :as gdom]
   [rum.core :as rum]))


(println "This text is printed from src/blog/core.cljs. Go ahead and edit it and see reloading in action.")

(defn multiply [a b] (* a b))


;; define your app data so that it doesn't get over-written on reload
(defonce app-state (atom {:text "Hello world!"}))


(defn get-app-element []
  (gdom/getElement "app"))

(rum/defc hello-world []
  [:div
   [:h1 (:text @app-state)]
   [:h3 "Edit this in src/blog/core.cljs and watch it change!"]])

(defn mount [el]
  (rum/mount (hello-world) el))

(defn mount-app-element []
  (when-let [el (get-app-element)]
    (mount el)))

;; conditionally start your application based on the presence of an "app" element
;; this is particularly helpful for testing this ns without launching the app
(mount-app-element)

;; specify reload hook with ^;after-load metadata
(defn ^:after-load on-reload []
  (mount-app-element))
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)



(comment
  (require 'cljs.repl.node)
  (cider.piggieback/cljs-repl (cljs.repl.node/repl-env)))


(ns blog.icons
  (:require [rum.core :as rum]))


(def ^:private ICON-SIZE-MD "32")  ; px
(def ^:private ICON-SIZE-SM "16")  ; px
(def ICON-COLOR-GRAY "#1a202c")  ; rgb
(def ICON-COLOR-WHITE "#ffffff")  ; rgb
(def ^:private ICON-STROKE "1")  ; px


(def ^:private icon-paths
  {:github [[:path
             {:d "M9 19c-4.286 1.35-4.286-2.55-6-3m12 5v-3.5c0-1 .099-1.405-.5-2 2.791-.3 5.5-1.366
                 5.5-6.04a4.567 4.567 0 0 0 -1.333 -3.21 4.192 4.192 0 00-.08-3.227s-1.05-.3-3.476
                 1.267a12.334 12.334 0 0 0 -6.222 0C6.462 2.723 5.413 3.023 5.413 3.023a4.192 4.192
                 0 0 0 -.08 3.227A4.566 4.566 0 004 9.486c0 4.64 2.709 5.68 5.5 6.014-.591.589-.56 1.183-.5 2V21"}]]
   :twitter [[:path
              {:d "M22 4.01c-1 .49-1.98.689-3 .99-1.121-1.265-2.783-1.335-4.38-.737S11.977 6.323 12
                  8v1c-3.245.083-6.135-1.395-8-4 0 0-4.182 7.433 4 11-1.872 1.247-3.739 2.088-6 2 3.308
                  1.803 6.913 2.423 10.034 1.517 3.58-1.04 6.522-3.723 7.651-7.742a13.84 13.84 0 0 0 .497
                  -3.753C20.18 7.773 21.692 5.25 22 4.009z"}]]
   :rss [[:circle {:cx "5" :cy "19" :r "1"}]
         [:path {:d "M4 4a16 16 0 0 1 16 16"}]
         [:path {:d "M4 11a9 9 0 0 1 9 9"}]]
   :external-link [[:path {:d "M11 7h-5a2 2 0 0 0 -2 2v9a2 2 0 0 0 2 2h9a2 2 0 0 0 2 -2v-5"}]
                   [:line {:x1 "10" :y1 "14" :x2 "20" :y2 "4"}]
                   [:polyline {:points "15 4 20 4 20 9"}]]})


(rum/defc ^:private icon-base
  [name icon-size color]
  [:svg
   {:xmlns "http://www.w3.org/2000/svg"
    :width icon-size
    :height icon-size
    :viewBox "0 0 24 24"
    :stroke-width ICON-STROKE
    :stroke color
    :fill "none"
    :stroke-linecap "round"
    :stroke-linejoin "round"}
   (cons
     [:path {:stroke "none"
             :d "M0 0h24v24H0z"}]
     (get icon-paths name))])


(rum/defc icon-md
  ([name]
   (icon-md name ICON-COLOR-GRAY))
  ([name color]
   (icon-base name ICON-SIZE-MD color)))


(rum/defc icon-sm
  ([name]
   (icon-sm name ICON-COLOR-GRAY))
  ([name color]
   (icon-base name ICON-SIZE-SM color)))


[:svg.icon.icon-tabler.icon-tabler-external-link
 {:xmlns "http://www.w3.org/2000/svg"
  :width "36" :height "36" :viewBox "0 0 24 24" :stroke-width "1.5" :stroke "#1a202c" :fill "none" :stroke-linecap "round" :stroke-linejoin "round"}
 [:path {:stroke "none" :d "M0 0h24v24H0z"}]
 [:path {:d "M11 7h-5a2 2 0 0 0 -2 2v9a2 2 0 0 0 2 2h9a2 2 0 0 0 2 -2v-5"}]
 [:line {:x1 "10" :y1 "14" :x2 "20" :y2 "4"}]
 [:polyline {:points "15 4 20 4 20 9"}]]
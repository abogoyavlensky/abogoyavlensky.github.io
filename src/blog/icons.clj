(ns blog.icons
  (:require [rum.core :as rum]))


(def ^:private ICON-SIZE "32")  ; px
(def ^:private ICON-COLOR "#1a202c")  ; rgb
(def ^:private ICON-STROKE "1")  ; px


(def GITHUB-ICON-NAME :github)
(def TWITTER-ICON-NAME :twitter)
(def RSS-ICON-NAME :rss)


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
         [:path {:d "M4 11a9 9 0 0 1 9 9"}]]})


(rum/defc icon
  [name]
  [:svg
   {:xmlns "http://www.w3.org/2000/svg"
    :width ICON-SIZE
    :height ICON-SIZE
    :viewBox "0 0 24 24"
    :stroke-width ICON-STROKE
    :stroke ICON-COLOR
    :fill "none"
    :stroke-linecap "round"
    :stroke-linejoin "round"}
   (cons
     [:path {:stroke "none"
             :d "M0 0h24v24H0z"}]
     (get icon-paths name))])

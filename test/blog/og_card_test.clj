(ns blog.og-card-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [blog.og-card :as og-card])
  (:import (java.awt Font)
           (java.awt.image BufferedImage)))


(defn- measure
  [text]
  (* 10 (count text)))


(deftest test-wrap-lines-short-text-ok
  (is (= ["Hello world"] (og-card/wrap-lines "Hello world" 200 measure))))


(deftest test-wrap-lines-long-text-ok
  (let [text "Deploying a full-stack Clojure app with Kamal on a single server"
        lines (og-card/wrap-lines text 250 measure)]
    (is (< 1 (count lines)))
    (is (every? #(<= (measure %) 250) lines))
    (is (= text (str/join " " lines)))))


(deftest test-wrap-lines-keeps-long-word-on-its-own-line-ok
  (is (= ["a" "verylongwordthatdoesnotfit" "b"]
        (og-card/wrap-lines "a verylongwordthatdoesnotfit b" 50 measure))))


(defn- font-rendering-available?
  "The CI image has no libfreetype, so the JDK cannot use fonts there.
  This probes only the JDK, so a bug in the renderer still fails the tests."
  []
  (try
    (let [image (BufferedImage. 1 1 BufferedImage/TYPE_INT_RGB)]
      (.getFontMetrics (.createGraphics image) (Font. Font/SANS_SERIF Font/PLAIN 12))
      true)
    (catch Throwable _
      false)))


(defn- white?
  [rgb]
  (= 0xFFFFFF (bit-and rgb 0xFFFFFF)))


(deftest test-render-card-ok
  (when (font-rendering-available?)
    (let [image (og-card/render-card "Deploying a full-stack Clojure app with Kamal on a single server")
          width (.getWidth image)
          height (.getHeight image)]
      (testing "uses the size declared in the meta tags"
        (is (= 1200 width))
        (is (= 630 height)))
      (testing "has a white background and drawn content"
        (is (white? (.getRGB image 0 0)))
        (is (not-every? white? (for [x (range 80 width 7)
                                     y (range 80 height 7)]
                                 (.getRGB image x y))))))))


(deftest test-render-card-title-too-long-fail
  (when (font-rendering-available?)
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"does not fit"
          (og-card/render-card (str/join " " (repeat 60 "title")))))))

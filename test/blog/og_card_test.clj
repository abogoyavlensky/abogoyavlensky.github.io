(ns blog.og-card-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [blog.og-card :as og-card]))


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
  "The CI image has no libfreetype, so drawing text fails there with a linkage error."
  []
  (try
    (og-card/render-card "Test")
    true
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

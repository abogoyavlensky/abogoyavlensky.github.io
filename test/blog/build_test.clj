(ns blog.build-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [blog.build :as build])
  (:import (java.awt.image BufferedImage)
           (java.nio.file Files)
           (java.nio.file.attribute FileAttribute)
           (javax.imageio ImageIO)))


(defn- temp-dir
  []
  (str (Files/createTempDirectory "og-override" (make-array FileAttribute 0))))


(defn- write-image!
  [dir file-name width height]
  (ImageIO/write (BufferedImage. width height BufferedImage/TYPE_INT_RGB)
    "png"
    (io/file dir file-name)))


(deftest test-check-og-override-valid-image-ok
  (let [dir (temp-dir)]
    (write-image! dir "card.png" 1200 630)
    (is (nil? (#'build/check-og-override! dir "card.png")))))


(deftest test-check-og-override-missing-file-fail
  (is (thrown-with-msg? clojure.lang.ExceptionInfo #"1200x630"
        (#'build/check-og-override! (temp-dir) "missing.png"))))


(deftest test-check-og-override-wrong-size-fail
  (let [dir (temp-dir)]
    (write-image! dir "small.png" 512 512)
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"1200x630"
          (#'build/check-og-override! dir "small.png")))))

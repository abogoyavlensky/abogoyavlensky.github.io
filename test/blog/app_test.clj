(ns blog.app-test
  (:require [clojure.test :refer [deftest is]]
            [blog.app :as app]))


(def ^:private OG-IMAGE
  "https://bogoyavlensky.com/assets/images/my_photo_512x512.JPG?v=1")


(deftest test-base-html-meta-og-image-ok
  (is (= OG-IMAGE (:og-image (#'app/base-html-meta "Blog" nil)))))


(deftest test-article-html-meta-og-image-ok
  (let [article {:title "Test"
                 :description "Test description"
                 :keywords ["test"]
                 :slug "test-slug"
                 :date "2026-06-08"}]
    (is (= OG-IMAGE (:og-image (#'app/article->html-meta article))))))

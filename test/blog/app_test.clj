(ns blog.app-test
  (:require [clojure.test :refer [deftest is]]
            [blog.app :as app]))


(def ^:private IMAGES-URL "https://bogoyavlensky.com/assets/images/")


(deftest test-base-html-meta-og-image-ok
  (is (= (str IMAGES-URL "og/site.png")
        (:og-image (#'app/base-html-meta "Blog" nil)))))


(def ^:private sample-article
  {:title "Test"
   :description "Test description"
   :keywords ["test"]
   :slug "test-slug"
   :date "2026-06-08"})


(deftest test-article-html-meta-og-image-ok
  (is (= (str IMAGES-URL "og/test-slug.png")
        (:og-image (#'app/article->html-meta sample-article)))))


(deftest test-article-html-meta-og-image-override-ok
  (let [article (assoc sample-article :og-image "articles/custom_og.png")]
    (is (= (str IMAGES-URL "articles/custom_og.png")
          (:og-image (#'app/article->html-meta article))))))

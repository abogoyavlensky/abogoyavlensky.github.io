(ns blog.pages-test
  (:require [clojure.test :refer [deftest is]]
            [blog.pages :as pages]))


(def ^:private sample-meta
  {:title "Blog"
   :description "Blog of Andrey Bogoyavlenskiy mostly about programming"
   :canonical "https://bogoyavlensky.com/"
   :og-type :website
   :og-image "https://bogoyavlensky.com/assets/images/my_photo_512x512.JPG?v=1"})


(deftest test-base-og-tags-social-image-ok
  (let [tags (set (#'pages/base-og-tags sample-meta))]
    (is (contains? tags [:meta {:property "og:image"
                                :content "https://bogoyavlensky.com/assets/images/my_photo_512x512.JPG?v=1"}]))
    (is (contains? tags [:meta {:property "og:image:width" :content "512"}]))
    (is (contains? tags [:meta {:property "og:image:height" :content "512"}]))
    (is (contains? tags [:meta {:name "twitter:card" :content "summary"}]))
    (is (contains? tags [:meta {:name "twitter:image"
                                :content "https://bogoyavlensky.com/assets/images/my_photo_512x512.JPG?v=1"}]))
    (is (contains? tags [:meta {:name "twitter:site" :content "@abogoyavlensky"}]))
    (is (contains? tags [:meta {:name "twitter:creator" :content "@abogoyavlensky"}]))))

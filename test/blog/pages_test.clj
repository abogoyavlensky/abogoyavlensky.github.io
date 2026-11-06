(ns blog.pages-test
  (:require [clojure.test :refer [deftest is]]
            [blog.pages :as pages]))


(def ^:private sample-meta
  {:title "Blog"
   :description "Blog of Andrey Bogoyavlenskiy mostly about programming"
   :canonical "https://bogoyavlensky.com/"
   :og-type :website
   :og-image "https://bogoyavlensky.com/assets/images/og/site.png"})


(deftest test-base-og-tags-social-image-ok
  (let [tags (set (#'pages/base-og-tags sample-meta))]
    (is (contains? tags [:meta {:property "og:image"
                                :content "https://bogoyavlensky.com/assets/images/og/site.png"}]))
    (is (contains? tags [:meta {:property "og:image:width" :content "1200"}]))
    (is (contains? tags [:meta {:property "og:image:height" :content "630"}]))
    (is (contains? tags [:meta {:name "twitter:card" :content "summary_large_image"}]))
    (is (contains? tags [:meta {:name "twitter:image"
                                :content "https://bogoyavlensky.com/assets/images/og/site.png"}]))
    (is (contains? tags [:meta {:name "twitter:site" :content "@abogoyavlensky"}]))
    (is (contains? tags [:meta {:name "twitter:creator" :content "@abogoyavlensky"}]))))

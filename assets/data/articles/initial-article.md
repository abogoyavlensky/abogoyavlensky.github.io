Consequat id porta nibh venenatis cras. Sapien nec sagittis aliquam malesuada.
Porta nibh venenatis cras sed felis eget velit aliquet sagittis. Et malesuada fames ac turpis egestas.

```shell script
$ lein deps :tree
```

```clojure
(defn demo
  []
  (let [name "test"]
    name))
```

### Another one

```clojure
(def ^:dynamic chunk-size 17)

(defn next-chunk [rdr]
  (let [buf (char-array chunk-size)
        s (.read rdr buf)]
  (when (pos? s)
    (java.nio.CharBuffer/wrap buf 0 s))))

(defn chunk-seq [rdr]
  (when-let [chunk (next-chunk rdr)]
    (cons chunk (lazy-seq (chunk-seq rdr)))))
```

Some more code blocks:

```clojure
(defn articles-list-data
  [site-data]
  (->> site-data
       :articles
       (sort-by :id >)
       (map #(select-keys % [:title :slug :date]))))


(defn- read-article-md-file
  [slug]
  (-> (format ARTICLE-DETAIL-PATH slug)
      (io/resource)
      (slurp)
      (markdown/md-to-html-string)))
```

It's a common practice for Single Page Applications (SPAs) to render the `index.html` page for any route on the server to establish the basic frontend setup. In this article, we'll explore how to achieve this in Clojure using the Reitit router.

The simplest approach is to handle `/*` with a resource handler pointed to `index.html`. While this approach works well, it can potentially lead to conflicts in routes due to the default wildcard route. So, we have to use `:conflicts nil` router option.

```clojure
(ns myprj.router
  (:require [ring.util.response :as ring-response]
            [reitit.ring :as ring-reitit]))

(defn- handler
  "Return main application handler."
  []
  (ring-reitit/ring-handler
    (ring-reitit/router
      [["/health" {:name ::health-check
                   :get {:handler (fn [_] (ring-response/response "OK"))}}]
       ["/*" {:name ::index
              :get {:handler (fn [_]
                               (-> "index.html"
                                   (ring-response/resource-response {:root "public"})
                                   (ring-response/content-type "text/html")))}}]]
      {:conflicts nil})))
```

Fortunately, in Reitit, there's a way to add a defalut route without encountering conflicts. For instance, you can utilize existing `create-resource-handler`, intended to render files from the resources directory of the project. It even has a `:index-files` parameter where you can specify the path to `index.html`. However, it redirects to `/index.html` on every request and there is no way to avoid it.

To render `index.html` on any request to the server without redirection, we can reuse a handler from above wrapped as a common handler:

```clojure
(ns myprj.util.handler
  (:require [ring.util.response :as ring-response]))

(defn create-index-handler
  "Create a handler to render index.html on any request."
  ([]
   (create-index-handler {}))
  ([{:keys [index-file root]
     :or {index-file "index.html"
          root "public"}}]
   (letfn [(index-handler-fn
             [_request]
             (-> index-file
                 (ring-response/resource-response {:root root})
                 (ring-response/content-type "text/html")))]
     (fn
       ([request]
        (index-handler-fn request))
       ([request respond _]
        (respond (index-handler-fn request)))))))
```

Now you can use this handler as follows:

```clojure
(ns myprj.router
  (:require [ring.util.response :as ring-response]
            [reitit.ring :as ring-reitit]
            [myprj.util.handler :as util-handler]))

(defn- handler
  "Return the main application handler."
  []
  (ring-reitit/ring-handler
    (ring-reitit/router
      [["/health" {:name ::health-check
                   :get {:handler (fn [_] (ring-response/response "OK"))}}]])
    (ring-reitit/routes
      (create-index-handler))))
```

If you have any other common resource handlers, be sure to place them before the index handler in your setup.

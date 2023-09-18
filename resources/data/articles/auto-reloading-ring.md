When developing Clojure applications locally, it is highly convenient to have automatic reloading of route handlers every time you make a code change. For Ring applications using Compojure or other routers, the `ring/ring-devel` library provides a helpful middleware known as [`wrap-reload`](https://github.com/ring-clojure/ring/blob/600e277e9c7a50fefb28c9be57b77e06dad5c84f/ring-devel/src/ring/middleware/reload.clj#L21). Regrettably, this middleware does not work seamlessly with the Reitit router.

Fortunately, starting from version `0.6.0`, Reitit introduced [`reloading-ring-handler`](https://github.com/metosin/reitit/blob/90f3708e16a5099d7a7e0e8ea30fefc04daeb24d/modules/reitit-ring/src/reitit/ring.cljc#L371C7-L391). However, it has a limitation - it does not actually re-evaluate the code when changes occur; instead, it only runs the existing handler.

To address this limitation and enable auto-reloading of handler code with Reitit, you can use a short snippet from current article. This snippet leverages the internal workings of `wrap-reload` and the approach taken by `reloading-ring-handler` in Reitit. Importantly, it is compatible with any server (e.g., Jetty, Immutant, etc.) and system component management library (e.g., Integrant, Component, Mount, etc.). It also works seamlessly with both middlewares and interceptors.

In your deps.edn or project.clj, make sure to include the `ring/ring-devel "1.10.0"` library with code-reloading utilities.
Afterwards, you can incorporate the reloading handler into your project:

```clojure
(ns myprj.util.middlewares
  (:require [ring.middleware.reload :as reload]))

(defn reloading-ring-handler
  "Reload ring handler on each request."
  [f]
  (let [reload! (#'reload/reloader ["src"] true)]
    (fn
      ([request]
       (reload!)
       ((f) request))
      ([request respond raise]
       (reload!)
       ((f) request respond raise)))))
```

Now, you can use it when creating a Reitit handler, for example, with Jetty. In this simple example, I have omitted any component system for simplicity.
Also, it is convenient to pass configuration option to enable auto-reloading only in dev environment.

```clojure
(ns myprj.router
  (:require [ring.adapter.jetty :as ring-jetty]
            [ring.util.response :as ring-response]
            [reitit.ring :as ring-reitit]
            [myprj.util.middlewares :as util-middlewares]))

(defn- handler
  "Return main application handler."
  []
  (ring-reitit/ring-handler
    (ring-reitit/router
      [["/health" {:name ::health-check
                   :get {:handler (fn [_] (ring-response/response "OK"))}}]])))

(defn run-server
  [{:keys [dev-mode? server-options]}]
  (let [create-handler-fn #(handler)
        handler* (if dev-mode?
                   (util-middlewares/reloading-ring-handler create-handler-fn)
                   (create-handler-fn))]
    (ring-jetty/run-jetty handler* server-options)))

(run-server {:dev-mode? true
             :server-options {:join? false :port 8000}}))
```

Then, for instance, it is possible to change the response from `"OK"` to `"Hello"` and this change will work for the next request without restarting the server.

That's all there is to it. I hope this will prove useful for your next Clojure project.

(ns blog.server
  (:require [com.stuartsierra.component :as component]
            [ring.adapter.jetty :as jetty]))


(defrecord Server [app options server]
  component/Lifecycle
  (start [this]
    (println "Starting server...")
    (if server
      this
      (assoc this :server (jetty/run-jetty (app) options))))
  (stop [this]
    (if (not server)
      (do (println "Server has already been stopped")
          this)
      (do
        (println "Stoping server...")
        (.stop server)
        (.join server)
        (assoc this :server nil)))))


(defn new-server
  ([app]
   (new-server app {}))
  ([app options]
   (map->Server {:app app
                 :options (merge {:port 8000 :join? false} options)})))

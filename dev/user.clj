(ns user
  (:require [clojure.tools.namespace.repl :refer [set-refresh-dirs]]
            [reloaded.repl :as reloaded]
            [ring.middleware.reload :refer [wrap-reload]]
            [blog.app :as app]))


(set-refresh-dirs "dev" "src")


(defn new-dev-system
  []
  (let [dev-app (fn []
                  (-> (app/app)
                      (wrap-reload)))]
    (app/new-system dev-app)))


(reloaded/set-init! #(new-dev-system))


(defn reset
  []
  (reloaded/reset))


(comment
  (reloaded/go)
  (reset)
  (reloaded/stop)

  (keys reloaded/system)
  (:server reloaded/system)
  (:http reloaded/system))

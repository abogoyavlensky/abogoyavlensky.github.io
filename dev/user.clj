(ns user
  (:require [clojure.tools.namespace.repl :refer [set-refresh-dirs]]
            [reloaded.repl :refer [system init start stop go reset-all]]
            [reloaded.repl :as reloaded]
            [ring.middleware.reload :refer [wrap-reload]]
            [blog.app :as app]))


(set-refresh-dirs "dev" "src")


;(reloaded.repl/set-init! #(app/new-system app/dev-app))

(defn new-dev-system
  []
  (let [dev-app (fn []
                  (-> (app/app)
                      (wrap-reload)))]
    (app/new-system dev-app)))


(reloaded.repl/set-init! #(new-dev-system))


(defn reset
  []
  (reloaded/reset))


(comment
  (go)
  (reset)
  (stop)

  (keys system)
  (:server system)
  (:http system))

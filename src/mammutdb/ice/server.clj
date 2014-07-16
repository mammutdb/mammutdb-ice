(ns mammutdb.ice.server
  (:require [compojure.handler :as handler]
            [compojure.core :refer [defroutes routes GET]]
            [compojure.route :as route]
            [ring.util.response :as response]
            [ring.adapter.jetty :refer [run-jetty]]))

(defn client-routes []
  (routes
   (GET "/" [] (response/resource-response "index.html" {:root "public"}))
   (route/resources "/")))

(defroutes main-routes (client-routes))
(def app (handler/site main-routes))

(defn run []
  (defonce ^:private server
    (run-jetty #'app {:port 3000 :join? false}))
  server)

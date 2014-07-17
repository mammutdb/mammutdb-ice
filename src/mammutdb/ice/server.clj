(ns mammutdb.ice.server
  (:require [compojure.handler :as handler]
            [compojure.core :refer [defroutes routes GET context]]
            [compojure.route :as route]
            [clojure.java.io :as io]
            [ring.adapter.jetty9 :refer [run-jetty]]
            [ring.util.response :as response])
  (:gen-class))


(def main-routes
  (routes
   (GET "/" []
     (slurp (io/resource "public/index.html")))
   (route/resources "/")))

(defroutes development-routes
  (GET "/" [] (response/redirect "/_ice/"))
  (context "/_ice" [] main-routes))

(defn run
  [join?]
  (let [app (handler/site development-routes)]
    (run-jetty app {:port 3000 :join? join?})))

(defn -main
  [& args]
  (run true))

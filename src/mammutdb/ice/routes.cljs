(ns mammutdb.ice.routes
  (:require [secretary.core :as sec :include-macros true :refer [defroute]]
            [goog.events :as ge]
            [cljs.core.async :refer [chan <! >! put! pub sub unsub unsub-all]]
            [mammutdb.ice.events :as events])
  (:import [goog History]
           [goog.history EventType]))

(defroute database-route "/database/:database" {:keys [database]}
  (.log js/console (str "Database: " database))
  (put! events/event-bus {:event :select-database :data {:database database}})
  (put! events/event-publisher {:event :set-database :data database})
  )

(defroute collection-route "/database/:database/:collection" {:keys [database collection]}
  (.log js/console (str "Database: " database))
  (.log js/console (str "Collection: " collection))
  (put! events/event-bus {:event :select-database :data {:database database}})
  (put! events/event-publisher {:event :set-database :data database})
  (put! events/event-bus {:event :select-collection :data {:collection collection}})
)

(def history (History.))

(ge/listen history EventType.NAVIGATE
  (fn [e] (sec/dispatch! (.-token e))))

(.setEnabled history true)

(defn set-hash [params]
  (.log js/console (str ">> " params))
  (if (or (nil? (:database params))
          (empty? (:database params)))
    (set! (-> js/window .-location .-hash) "/")
    (if (:collection params)
      (set! (-> js/window .-location .-hash) (collection-route params))
      (set! (-> js/window .-location .-hash) (database-route params)))))

(ns mammutdb.ice.http
  (:require [goog.events :as events]
            [goog.dom :as gdom])
  (:import [goog.net XhrIo]
           goog.net.EventType
           [goog.events EventType]))

(def ^:private meths
  {:get "GET"
   :put "PUT"
   :post "POST"
   :delete "DELETE"})

(defn json-xhr [{:keys [method url data on-complete on-error]}]
  (let [xhr (XhrIo.)]
    (events/listen
     xhr goog.net.EventType.SUCCESS
     (fn [e]
       (on-complete
        (let [response (.getResponseText xhr)
              response-obj (.parse js/JSON response)]
          (js->clj response-obj :keywordize-keys true)))))
    (events/listen
     xhr goog.net.EventType.ERROR
     (fn [e]
       (on-error {:error (.getResponseText xhr)})))
    (. xhr
       (send url (meths method)
             (when data (.stringify js/JSON (clj->js data)))
             #js {"Content-Type" "application/json" "Accept" "application/json"}))))

(ns mammutdb.ice.http
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [goog.events :as events]
            [goog.dom :as gdom]
            [cljs.core.async :refer [chan >! <!]]
            [mammutdb.ice.state :as state])
  (:import [goog.net XhrIo]
           goog.net.EventType
           [goog.events EventType]))

(def ^:private meths
  {:get "GET"
   :put "PUT"
   :post "POST"
   :delete "DELETE"})

(defn json-xhr [{:keys [method url data on-complete on-error]}]
  (try
    (let [xhr (XhrIo.)]
      (events/listen
       xhr goog.net.EventType.SUCCESS
       (fn [e]
         (on-complete
          (let [response (.getResponseText xhr)
                response-obj (if (not-empty response) (.parse js/JSON response) "")]
            (js->clj response-obj :keywordize-keys true)))))
      (events/listen
       xhr goog.net.EventType.ERROR
       (fn [e]
         (on-error (str (.formatMsg_ xhr "Sending resquest") (.getResponseText xhr)))))
      (. xhr
         (send url (meths method)
               (when data (.stringify js/JSON (clj->js data)))
               #js {"Content-Type" "application/json" "Accept" "application/json"})))
    (catch js/Error e
      (on-error (str (.-message e))))))

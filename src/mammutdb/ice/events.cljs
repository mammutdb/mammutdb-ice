(ns mammutdb.ice.events
  (:require [mammutdb.ice.http :as http]
            [mammutdb.ice.state :as state]
            [cljs.core.async :refer [chan <! >! put! pub sub unsub unsub-all]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

; (def base-url "http://mammutdb.apiary-mock.com")
(def base-url "http://localhost:9002")

;; Event publication
(def event-bus (chan))
(def event-publisher (chan))
(def event-publication (pub event-publisher :event))

;; Event processing
(defmulti process-event :event)

(defmethod process-event :load-databases [event]
  (.log js/console "Load databases")
  (http/json-xhr {:method :get
                  :url (str base-url "/")
                  :on-complete (fn [result]
                                 (.log js/console "Returned get: " result)
                                 (put! event-publisher {:event :result-databases :data result}))
                  :on-error (fn [result] (.log js/console (str result)))}))

(defmethod process-event :select-database [event]
  (.log js/console "Database selected")
  (let [value (-> event :data :database)
        value (if (empty? value) nil value)]
    (swap! state/app assoc :selected-database value)
    (swap! state/app dissoc :selected-collection)
    (put! event-publisher {:event :result-collections :data []})
    (swap! state/app dissoc :selected-document)
    (put! event-publisher {:event :result-documents :data []})

    (when value
      (http/json-xhr {:method :get
                      :url (str base-url "/" (:selected-database @state/app))
                      :on-complete (fn [result]
                                     (.log js/console "Returned get: " result)
                                     (put! event-publisher {:event :result-collections :data result}))
                      :on-error (fn [result] (.log js/console (str result)))}))))

(defmethod process-event :select-collection [event]
  (.log js/console "Collection selected")
  (swap! state/app assoc :selected-collection (-> event :data :collection))
  (swap! state/app dissoc :selected-document)
  (put! event-publisher {:event :result-documents :data []})

  (http/json-xhr {:method :get
                  :url (str base-url "/" (:selected-database @state/app) "/" (:selected-collection @state/app))
                  :on-complete (fn [result]
                                 (.log js/console "Returned get: " result)
                                 (put! event-publisher {:event :result-documents :data result}))
                  :on-error (fn [result] (.log js/console (str result)))}))

(defmethod process-event :create-database [event]
  (http/json-xhr {:method :put
                  :url (str base-url "/" (-> event :data :database))
                  :on-complete (fn [result]
                                 (.log js/console "Returned put: " result)
                                 (state/add-database! result)
                                 (put! event-publisher {:event :set-database :data result})
                                 (process-event {:event :select-database :data (:data event)}))
                  :on-error (fn [result] (.log js/console (str result)))}))

(defn start-event-loop []
  (.log js/console "Starting event loop")
  (go-loop []
    (let [event (<! event-bus)]
      (.log js/console (str event))
      (process-event event))
    (recur)))

(start-event-loop)

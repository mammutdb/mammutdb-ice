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
  (http/json-xhr {:method :get
                  :url (str base-url "/")
                  :on-complete (fn [result]
                                 (put! event-publisher {:event :result-databases :data result}))
                  :on-error state/set-error!}))

(defmethod process-event :select-database [event]
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
                                     (put! event-publisher {:event :result-collections :data result}))
                      :on-error state/set-error!}))))

(defmethod process-event :select-collection [event]
  (swap! state/app assoc :selected-collection (-> event :data :collection))
  (swap! state/app dissoc :selected-document)
  (put! event-publisher {:event :result-documents :data []})

  (http/json-xhr {:method :get
                  :url (str base-url
                            "/" (:selected-database @state/app)
                            "/" (:selected-collection @state/app))
                  :on-complete (fn [result]
                                 (put! event-publisher {:event :result-documents :data result}))
                  :on-error state/set-error!}))

(defmethod process-event :query-collection [{data :data}]
  (swap! state/app dissoc :selected-document)
  (put! event-publisher {:event :result-documents :data []})

  (http/json-xhr {:method :post
                  :url (str base-url
                            "/" (:selected-database @state/app)
                            "/" (:selected-collection @state/app)
                            "/query")
                  :data data
                  :on-complete (fn [result]
                                 (put! event-publisher {:event :result-documents :data result}))
                  :on-error state/set-error!}))

(defmethod process-event :create-database [event]
  (http/json-xhr {:method :put
                  :url (str base-url "/" (-> event :data :database))
                  :on-complete (fn [result]
                                 (state/add-database! result)
                                 (put! event-publisher {:event :set-database :data result})
                                 (process-event {:event :select-database :data (:data event)}))
                  :on-error state/set-error!}))

(defmethod process-event :create-collection [event]
  (http/json-xhr {:method :put
                  :url (str base-url "/" (:selected-database @state/app) "/" (-> event :data :collection))
                  :on-complete (fn [result]
                                 (state/add-collection! result)
                                 (put! event-publisher {:event :set-collection :data result})
                                 (process-event {:event :select-collection :data (:data event)}))
                  :on-error state/set-error!}))

(defmethod process-event :create-document [event]
  (http/json-xhr {:method :post
                  :url (str base-url "/" (:selected-database @state/app) "/" (:selected-collection @state/app))
                  :data (-> event :data :document)
                  :on-complete (fn [result]
                                 (state/add-document! result)
                                 (put! event-publisher {:event :refresh-documents}))
                  :on-error state/set-error!}))

(defmethod process-event :select-document [{data :data}]
  (state/displaying-document! (:document-id data))
  (http/json-xhr {:method :get
                  :url (str base-url
                            "/" (:selected-database @state/app)
                            "/" (:selected-collection @state/app)
                            "/" (:document-id data)
                            "/revs")
                  :on-complete (fn [result]
                                 (state/set-revs! result)
                                 (put! event-publisher {:event :refresh-documents}))
                  :on-error state/set-error!}))


(defmethod process-event :remove-database [{database-id :data}]
  (.log js/console (str "Removing database " database-id))
  (http/json-xhr {:method :delete
                  :url (str base-url "/" database-id)
                  :on-complete (fn [result]
                                 (state/remove-database! database-id))
                  :on-error state/set-error!}))

(defmethod process-event :remove-collection [{collection-id :data}]
  (.log js/console (str "Removing collection " collection-id))
  (http/json-xhr {:method :delete
                  :url (str base-url
                            "/" (:selected-database @state/app)
                            "/" collection-id)
                  :on-complete (fn [result]
                                 (state/remove-collection! collection-id)
                                 )
                  :on-error state/set-error!}))

(defmethod process-event :remove-document [{document-id :data}]
  (.log js/console (str "Removing document " document-id))
  (http/json-xhr {:method :delete
                  :url (str base-url
                            "/" (:selected-database @state/app)
                            "/" (:selected-collection @state/app)
                            "/" document-id)
                  :on-complete (fn [result]
                                 (state/remove-document! document-id)
                                 (put! event-publisher {:event :refresh-documents}))
                  :on-error state/set-error!}))


(defn start-event-loop []
  (go-loop []
    (let [event (<! event-bus)]
      (process-event event))
    (recur)))

(start-event-loop)

(ns mammutdb.ice.main
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [mammutdb.ice.http :as http]
            [cljs.core.async :refer [chan <! >! put! pub sub unsub unsub-all]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(enable-console-print!)

(def base-url "http://mammutdb.apiary-mock.com")

(def app-state
  (atom {; :databases [{:name "great_databases"}, {:name "piweek_winners"}]
         ; :collections [{:name "functional_databases"}, {:name "nosql_databases"}, {:name "sql_databases"}]
         ; :items (apply vector (map #(hash-map :id % :rev (str "1111-1111-" %) :createdAt "2014-01-01 12:00") (range 1 10)))

         :databases []
         :collections []
         :items []
         :show-query false
         }))

;; Event publication
(def event-bus (chan))
(def event-publisher (chan))
(def event-publication (pub event-publisher #(:event %)))

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
  (swap! app-state assoc :selected-database (-> event :data :database))
  (swap! app-state dissoc :selected-collection)
  (put! event-publisher {:event :result-collections :data []})
  (swap! app-state dissoc :selected-document)
  (put! event-publisher {:event :result-documents :data []})

  (http/json-xhr {:method :get
                  :url (str base-url "/" (:selected-database @app-state))
                  :on-complete (fn [result]
                                 (.log js/console "Returned get: " result)
                                 (put! event-publisher {:event :result-collections :data result}))
                  :on-error (fn [result] (.log js/console (str result)))}))

(defmethod process-event :select-collection [event]
  (.log js/console "Collection selected")
  (swap! app-state assoc :selected-collection (-> event :data :collection))
  (swap! app-state dissoc :selected-document)
  (put! event-publisher {:event :result-documents :data []})

  (http/json-xhr {:method :get
                  :url (str base-url "/" (:selected-database @app-state) "/" (:selected-collection @app-state))
                  :on-complete (fn [result]
                                 (.log js/console "Returned get: " result)
                                 (put! event-publisher {:event :result-documents :data result}))
                  :on-error (fn [result] (.log js/console (str result)))}))

(defn start-event-loop []
  (.log js/console "Starting event loop")
  (go-loop []
    (let [event (<! event-bus)]
      (.log js/console (str event))
      (process-event event))
    (recur)))

(start-event-loop)


;; DATABASE LIST
(defn database-view
  "Renders the view for a database element"
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/option #js {:value (:name data)} (:name data)))))

(defn database-select-view
  "Renders a database elements list returned from the server"
  [data owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (put! event-bus {:event :load-databases})
      (let [event-subscriber (chan)]
        (sub event-publication :result-databases event-subscriber)
        (go-loop []
          (let [{result :data} (<! event-subscriber)]
            (.log js/console (str result))
            (om/update! data :databases result))
          (recur))))
    om/IRender
    (render [this]
      (dom/div #js {:className "database-container"}
               (dom/h4 nil "Databases")
               (dom/button #js {:onClick (fn [e] (put! event-bus {:event :new-database}))
                                :className "tiny"} "Add New")
               (dom/button #js {:className "tiny"} "Delete current")
               (if (empty? (:databases data))
                 (dom/p nil "No databases found")
                 (apply dom/select #js {:name "database"
                                        :onChange (fn [e]
                                                    (let [selected (->> (.-target e)
                                                                        (.-value))]
                                                      (put! event-bus {:event :select-database :data {:database selected}})))}
                        (into [(dom/option nil "-- empty --")]
                              (om/build-all database-view (:databases data)))))
               ))))

(om/root
  database-select-view
  app-state
  {:target (. js/document (getElementById "database-select-view"))})

;; COLLECTIONS
(defn collection-view [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/li #js {:className "collection-list-item"}
              (dom/a #js {:className "collection-link"
                          :onClick (partial (fn [selected e]
                                              ; (om/update! data :selected-collection collection)
                                              ; (request-documents data database collection)
                                              (put! event-bus {:event :select-collection :data {:collection selected}})
                                              ) (:name data))}
                     (:name data))
              (dom/a #js {:className "close-btn"} "x")))))

(defn collection-list-view [data owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (let [event-subscriber (chan)]
        (sub event-publication :result-collections event-subscriber)
        (go-loop []
          (let [{result :data} (<! event-subscriber)]
            (.log js/console (str result))
            (om/update! data :collections result))
          (recur))))
    om/IRender
    (render [this]
      (dom/div #js {:className "collections-container"}
               (dom/h4 nil "Collections")
               (dom/button #js {:className "tiny"} "Add New")
               (if (empty? (:collections data))
                 (dom/p nil "No collections found")
                 (apply dom/ul #js {:className "side-nav collections"}
                        (om/build-all collection-view (:collections data))))))))

(om/root
  collection-list-view
  app-state
  {:target (. js/document (getElementById "collection-list-view"))})


;; ITEMS
(defn item-view [data owner]
  (letfn [(create-item [label field]
            (dom/div nil
                     (dom/span #js {:className "label"} label)
                     (dom/span #js {:className "field"} (field data))))]
    (reify
      om/IRender
      (render [this]
        (dom/div #js {:className "col-item"}
                 (create-item "ID" :id)
                 (create-item "REV" :rev)
                 (create-item "Created" :createdAt))))))

(defn items-list-view [data owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (let [event-subscriber (chan)]
        (sub event-publication :result-documents event-subscriber)
        (go-loop []
          (let [{result :data} (<! event-subscriber)]
            (.log js/console (str result))
            (om/update! data :items result))
          (recur))))
    om/IRender
    (render [this]
      (if (empty? (:items data))
        (dom/p nil "No items found")
        (apply dom/div #js {:className "collection"}
               (om/build-all item-view (:items data)))))))

;; QUERY PANEL
(defn query-panel-view [data owner]
  (letfn [(get-query-class []
            (str "query" " " (if (:show-query data) "" "collapsed")))
          (toggle-query-visibility []
            (om/transact! data :show-query (fn [v] (not v))))]
    (reify
      om/IRender
      (render[this]
        (dom/div #js {:className "large-9 push-3 columns"}
                 (dom/h3 nil "Collection: Cool databases")
                 (dom/div #js {:className (get-query-class)}
                          (dom/a #js {:className "hide-btn" :onClick toggle-query-visibility} "[-] hide")
                          (dom/a #js {:className "show-btn" :onClick toggle-query-visibility} "[+] show")
                          (dom/textarea))
                 (om/build items-list-view data))))))

(om/root
 query-panel-view
 app-state
 {:target (. js/document (getElementById "query-panel-view"))})

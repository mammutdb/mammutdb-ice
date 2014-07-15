(ns mammutdb.ice.main
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [chan <! >! put! pub sub unsub unsub-all]]
            [mammutdb.ice.modals :as modals]
            [mammutdb.ice.state :as state]
            [mammutdb.ice.events :refer [event-bus event-publisher event-publication]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(enable-console-print!)

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
               (dom/a #js {:data-reveal-id "new-database-modal"
                           :className "button tiny"} "New database")
               (dom/a #js {:className "button tiny"} "Delete")
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
  state/app
  {:target (. js/document (getElementById "database-select-view"))})

;; COLLECTIONS
(defn collection-view [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/li #js {:className "collection-list-document"}
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
               (dom/a #js {:data-reveal-id "new-collection-modal"
                           :className "button tiny"} "New Collection")
               (if (empty? (:collections data))
                 (dom/p nil "No collections found")
                 (apply dom/ul #js {:className "side-nav collections"}
                        (om/build-all collection-view (:collections data))))))))

(om/root
  collection-list-view
  state/app
  {:target (. js/document (getElementById "collection-list-view"))})


;; DOCUMENTS
(defn documents-view [data owner]
  (letfn [(create-document [label field]
            (dom/div nil
                     (dom/span #js {:className "label"} label)
                     (dom/span #js {:className "field"} (field data))))]
    (reify
      om/IRender
      (render [this]
        (dom/div #js {:className "col-document"}
                 (create-document "ID" :_id)
                 (create-document "REV" :_rev)
                 (create-document "Created" :_createdat))))))

(defn documents-list-view [data owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (let [event-subscriber (chan)]
        (sub event-publication :result-documents event-subscriber)
        (go-loop []
          (let [{result :data} (<! event-subscriber)]
            (.log js/console (str result))
            (om/update! data :documents result))
          (recur))))
    om/IRender
    (render [this]
      (if (empty? (:documents data))
        (dom/p nil "No documents found")
        (apply dom/div #js {:className "collection"}
               (om/build-all documents-view (:documents data)))))))

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
                 (dom/div #js {:className "collection-title"}
                          (dom/h3 nil "Collection: Cool databases")
                          (dom/a #js {:data-reveal-id "new-document-modal"
                                      :className "button tiny"} "New Document"))
                 (dom/div #js {:className (get-query-class)}
                          (dom/a #js {:className "hide-btn" :onClick toggle-query-visibility} "[-] hide")
                          (dom/a #js {:className "show-btn" :onClick toggle-query-visibility} "[+] query")
                          (dom/textarea)
                          (dom/hr nil))
                 (om/build documents-list-view data))))))

(om/root
 query-panel-view
 state/app
 {:target (. js/document (getElementById "query-panel-view"))})

(ns mammutdb.ice.main
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [chan <! >! put! pub sub unsub unsub-all]]
            [cats.monad.maybe :as maybe]
            [cats.core :as m]
            [cats.monad.either :as either]
            [mammutdb.ice.parser :as p]
            [mammutdb.ice.modals :as modals]
            [mammutdb.ice.errors :as errors]
            [mammutdb.ice.state :as state]
            [mammutdb.ice.events :refer [event-bus event-publisher event-publication]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]]))

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
    (will-mount [this]
      (put! event-bus {:event :load-databases})
      (let [result-event-subscriber (chan)
            refresh-event-subscriber (chan)]
        (sub event-publication :result-databases result-event-subscriber)
        (sub event-publication :set-database refresh-event-subscriber)

        (go-loop []
          (alt!
            result-event-subscriber ([{event-data :data}]
                                       (om/update! data :databases event-data))
            refresh-event-subscriber ([{event-data :data}]
                                        (set! (.-value (om/get-node owner "databaseList")) (if (nil? event-data) nil (:name event-data)))))
          (recur))))
    om/IRender
    (render [this]
      (dom/div #js {:className "database-container"}
               (dom/h4 nil "Databases")
               (dom/a #js {:data-reveal-id "new-database-modal"
                           :className "button tiny"} "New database")
               (dom/a #js {:onClick (partial (fn [database-id e]
                                               (put! event-bus {:event :remove-database :data database-id}))
                                             (:selected-database data))
                           :className "button tiny"} "Delete")
               (if (empty? (:databases data))
                 (dom/p nil "No databases found")
                 (apply dom/select #js {:name "database"
                                        :ref "databaseList"
                                        :onChange (fn [e]
                                                    (let [selected (->> (.-target e)
                                                                        (.-value))]
                                                      (put! event-bus {:event :select-database :data {:database selected}})))}
                        (into [(dom/option #js {:value ""} "-- empty --")]
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
              (dom/a #js {:onClick (partial (fn [collection-id e]
                                              (put! event-bus {:event :remove-collection :data collection-id})) (:id data))
                          :className "close-btn"
                          :dangerouslySetInnerHTML #js {:__html "&#215;"}})))))

(defn collection-list-view [data owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (let [event-subscriber (chan)]
        (sub event-publication :result-collections event-subscriber)
        (go-loop []
          (let [{result :data} (<! event-subscriber)]
            (om/update! data :collections result))
          (recur))))
    om/IRender
    (render [this]
      (apply dom/div #js {:className "collections-container"}
             (if (:selected-database data)
               [(dom/h4 nil "Collections")
                (dom/a #js {:data-reveal-id "new-collection-modal"
                            :className "button tiny"} "New Collection")
                (if (empty? (:collections data))
                  (dom/p nil "No collections found")
                  (apply dom/ul #js {:className "side-nav collections"}
                         (om/build-all collection-view (:collections data))))]
               [(dom/p nil "Seleccione una base de datos")])))))

(om/root
  collection-list-view
  state/app
  {:target (. js/document (getElementById "collection-list-view"))})


;; DOCUMENTS
(defn documents-view [data owner]
  (let [create-document (fn [element label field]
                           (dom/div nil
                                    (dom/span #js {:className "label"} label)
                                    (dom/span #js {:className "field"} (field element))))
        select-document (fn [id _]
                           (put! event-bus {:event :select-document :data {:document-id id}}))]
    (reify
      om/IInitState
      (init-state [_]
        {:rev-idx 0
         :exit-channel (chan)})

      om/IWillMount
      (will-mount [_]
        (let [refresh-event-subscriber (chan)
              exit-channel (om/get-state owner :exit-channel)]
          (sub event-publication :refresh-documents refresh-event-subscriber)
          (go-loop []
            (when (alt!
                    refresh-event-subscriber ([_] (try
                                                    (om/refresh! owner)
                                                    (boolean true)
                                                    (catch js/Error e
                                                      (boolean false))))
                    exit-channel ([_] (unsub event-publication :refresh-documents refresh-event-subscriber)(boolean false)))
              (recur)))))

      om/IWillUnmount
      (will-unmount [_]
        (let [exit-channel (om/get-state owner :exit-channel)]
          (put! exit-channel {})))

      om/IRenderState
      (render-state [this state]
        (let [document (if (= (:rev-idx state) 0) data (-> @state/app :displaying-revs (nth (:rev-idx state) data)))
              disabled-next (<= (:rev-idx state) 0)
              disabled-prev (>= (:rev-idx state) (dec (-> @state/app :displaying-revs count)))]
          (apply dom/div #js {:onMouseOver (partial select-document (:_id data))
                              :className "col-document"}
                 (create-document document "ID" :_id)
                 (create-document document "REV" :_rev)
                 (create-document document "Created" :_createdat)
                 (if (= (:displaying-document @state/app) (:_id data))
                   (let [data-text (-> document
                                       (dissoc :_id)
                                       (dissoc :_rev)
                                       (dissoc :_createdat)
                                       (clj->js)
                                       (#(.stringify js/JSON % nil 2)))]
                     [(dom/ul #js {:className "button-group"}
                              (dom/li nil (dom/a #js {:data-reveal-id "update-document-modal"
                                                      :onClick (partial (fn [id e]
                                                                          (state/edit-document! id data-text)
                                                                          (put! event-publisher {:event :refresh-modal}))
                                                                        (:_id data))
                                                      :className "button tiny"} "Editar"))
                              (dom/li nil (dom/a #js {:onClick (partial (fn [id e]
                                                                          (put! event-bus {:event :remove-document :data id}))
                                                                        (:_id data))
                                                      :className "button tiny"} "Borrar"))
                              (dom/li nil (dom/a #js {:disabled disabled-prev
                                                      :onClick (fn [e] (when (not disabled-prev) (om/set-state! owner :rev-idx (inc (:rev-idx state)))))
                                                      :className "button tiny"} "Anterior"))
                              (dom/li nil (dom/a #js {:disabled disabled-next
                                                      :onClick (fn [e] (when (not disabled-next) (om/set-state! owner :rev-idx (dec (:rev-idx state)))))
                                                      :className "button tiny"} "Siguiente")))
                      (dom/pre #js {:dangerouslySetInnerHTML #js {:__html data-text}
                                    :className "json"})]))))))))

(defn documents-list-view [data owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (let [event-subscriber (chan)
            refresh-event-subscriber (chan)]
        (sub event-publication :result-documents event-subscriber)
        (sub event-publication :refresh-documents refresh-event-subscriber)
        (go-loop []
          (alt!
            event-subscriber ([{result :data}] (om/update! data :documents result))
            refresh-event-subscriber ([_] (om/refresh! owner)))
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
            (om/transact! data :show-query (fn [v] (not v))))
          (remove-empty-vals [curmap]
            (letfn [(remove-empty [[_ val]]
                      (if (= (type val) js/String)
                        (and (not (nil? val)) (not-empty val))
                        (not (nil? val))))]
              (into {} (filter remove-empty curmap))))
          (query-documents [e]
            (let [query-text (.-value (om/get-node owner "queryInput"))
                  validation-result
                  (m/>>= (if (empty? query-text) (either/right nil) (p/txt->clj query-text))
                         (fn [input-query]
                           (let [input-ordering (.-value (om/get-node owner "orderingInput"))
                                 input-drop     (.-value (om/get-node owner "dropInput"))
                                 input-take     (.-value (om/get-node owner "takeInput"))
                                 event-data (remove-empty-vals {:filter input-query
                                                                :ordering input-ordering
                                                                :drop input-drop
                                                                :take input-take})]
                             (put! event-bus {:event :query-collection :data event-data})
                             (either/right nil))))]
              (when (either/left? validation-result)
                (js/alert (str ">> " (either/from-either validation-result))))))]
    (reify
      om/IRender
      (render[this]
        (apply dom/div #js {:className "large-9 push-3 columns"}
               (if (data :selected-collection)
                 [(dom/div #js {:className "collection-title"}
                           (dom/h3 nil (str "Collection: " (:selected-collection data)))
                           (dom/a #js {:data-reveal-id "new-document-modal"
                                       :className "button tiny"} "New Document"))
                  (dom/div #js {:className (get-query-class)}
                           (dom/a #js {:className "hide-btn" :onClick toggle-query-visibility} "[-] hide")
                           (dom/a #js {:className "show-btn" :onClick toggle-query-visibility} "[+] query")
                           (dom/div #js {:className "query-panel"}
                                    (dom/textarea #js {:ref "queryInput"})
                                    (dom/label nil "Ordering: " (dom/input #js {:ref "orderingInput"}))
                                    (dom/label nil "Drop: " (dom/input #js {:ref "dropInput"}))
                                    (dom/label nil "Take: " (dom/input #js {:ref "takeInput"}))
                                    (dom/a #js {:onClick query-documents
                                                :className "button tiny"} "Do stuff"))
                           (dom/hr nil))
                  (om/build documents-list-view data)]
                 [(dom/p nil "Seleccione una colección")]))))))

(om/root
 query-panel-view
 state/app
 {:target (. js/document (getElementById "query-panel-view"))})

(ns mammutdb.ice.modals
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cats.monad.either :as either]
            [cats.core :as m]
            [cljs.core.async :refer [chan <! >! put! pub sub unsub unsub-all]]
            [mammutdb.ice.jquery :as jquery]
            [mammutdb.ice.state :as state]
            [mammutdb.ice.events :refer [event-bus event-publisher event-publication]]
            [mammutdb.ice.validation :as valid])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(defn database-modal-view [data owner]
  (reify
    om/IInitState
    (init-state [_] {:error nil})
    om/IRenderState
    (render-state [this state]
      (letfn [(handle-create-database [_]
                (let [database-name (.-value (om/get-node owner "databaseName"))
                      create-data {:database database-name}
                      validation-result (m/>>= (valid/validate-create-data :database create-data)
                                               (fn [valid-data]
                                                 (om/set-state! owner :error "")
                                                 (put! event-bus {:event :create-database :data create-data})
                                                 (jquery/close-modal "new-database-modal")
                                                 (set! (.-value (om/get-node owner "databaseName")) "")
                                                 (either/right)))]
                  (when (either/left? validation-result)
                    (om/set-state! owner :error (.-v validation-result)))))]
        (dom/div nil
                 (dom/p #js {:className "lead"} "Introduce el nombre de la base de datos")
                 (dom/p #js {:className "error"} (:error state))
                 (dom/form nil
                           (dom/label nil "Database:")
                           (dom/input #js {:ref "databaseName"
                                           :type "text"}))
                 (dom/a #js {:dangerouslySetInnerHTML #js {:__html "&#215;"}
                             :className "close-reveal-modal"} nil)
                 (dom/ul #js {:className "button-group"}
                         (dom/li nil (dom/a #js {:onClick handle-create-database
                                                 :className "button small"} "Aceptar"))
                         (dom/li nil (dom/a #js {:onClick #(jquery/close-modal "new-database-modal")
                                                 :className "button small alert"} "Cancelar"))))))))

(om/root
 database-modal-view
 state/app
 {:target (. js/document (getElementById "new-database-modal"))})


(defn collection-modal-view [data owner]
  (reify
    om/IRender
    (render [_]
      (letfn [(handle-create-collection [_]
                (let [collection-name (.-value (om/get-node owner "collectionName"))
                      collection-type (.-value (om/get-node owner "collectionType"))]
                  (put! event-bus {:event :create-collection :data {:collection collection-name
                                                                    :type collection-type}})
                  (jquery/close-modal "new-collection-modal")
                  (set! (.-value (om/get-node owner "collectionName")) "")))]
        (dom/div nil
                 (dom/p #js {:className "lead"} "Introduce la colección y el tipo")
                 (dom/form nil
                           (dom/label nil "Collection:")
                           (dom/input #js {:ref "collectionName"
                                           :type "text"})
                           (dom/label nil "Type:")
                           (dom/select #js {:ref "collectionType"}
                                       (dom/option #js {:value "json"} "JSON")))
                 (dom/a #js {:dangerouslySetInnerHTML #js {:__html "&#215;"}
                             :className "close-reveal-modal"} nil)
                 (dom/ul #js {:className "button-group"}
                         (dom/li nil (dom/a #js {:onClick handle-create-collection
                                                 :className "button small"} "Aceptar"))
                         (dom/li nil (dom/a #js {:onClick #(jquery/close-modal "new-collection-modal")
                                                 :className "button small alert"} "Cancelar"))))))))

(om/root
 collection-modal-view
 state/app
 {:target (. js/document (getElementById "new-collection-modal"))})


(defn document-modal-view [data owner]
  (reify
    om/IRender
    (render [_]
      (letfn [(handle-create-document [_]
                (let [document-body
                      (->> "documentBody"
                           (om/get-node owner)
                           (.-value)
                           (.parse js/JSON)
                           (js->clj))]
                  (put! event-bus {:event :create-document :data {:document document-body}})
                  (jquery/close-modal "new-document-modal")
                  (set! (.-value (om/get-node owner "documentBody")) "")))]
        (dom/div nil
                 (dom/p #js {:className "lead"} "Introduce el documento (JSON)")
                 (dom/form nil
                           (dom/textarea #js {:ref "documentBody"}))
                 (dom/a #js {:dangerouslySetInnerHTML #js {:__html "&#215;"}
                             :className "close-reveal-modal"} nil)
                 (dom/ul #js {:className "button-group"}
                         (dom/li nil (dom/a #js {:onClick handle-create-document
                                                 :className "button small"} "Aceptar"))
                         (dom/li nil (dom/a #js {:onClick #(jquery/close-modal "new-document-modal")
                                                 :className "button small alert"} "Cancelar"))))))))

(om/root
 document-modal-view
 state/app
 {:target (. js/document (getElementById "new-document-modal"))})


(ns mammutdb.ice.modals
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [chan <! >! put! pub sub unsub unsub-all]]
            [mammutdb.ice.state :as state]
            [mammutdb.ice.events :refer [event-bus event-publisher event-publication]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(defn database-modal-view [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (dom/p #js {:className "lead"} "Introduce el nombre de la base de datos")
               (dom/form nil
                         (dom/label nil "Database:")
                         (dom/input #js {:type "text"}))
               (dom/a #js {:dangerouslySetInnerHTML #js {:__html "&#215;"}
                           :className "close-reveal-modal"} nil)
               (dom/ul #js {:className "button-group"}
                       (dom/li nil (dom/a #js {:className "button small"} "Aceptar"))
                       (dom/li nil (dom/a #js {:className "button small alert"} "Cancelar")))))))

(om/root
 database-modal-view
 state/app
 {:target (. js/document (getElementById "new-database-modal"))})


(defn collection-modal-view [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (dom/p #js {:className "lead"} "Introduce la colección y el tipo")
               (dom/form nil
                         (dom/label nil "Collection:")
                         (dom/input #js {:type "text"})
                         (dom/label nil "Type:")
                         (dom/select nil
                                     (dom/option #js {:value "json"} "JSON")))
               (dom/a #js {:dangerouslySetInnerHTML #js {:__html "&#215;"}
                           :className "close-reveal-modal"} nil)
               (dom/ul #js {:className "button-group"}
                       (dom/li nil (dom/a #js {:className "button small"} "Aceptar"))
                       (dom/li nil (dom/a #js {:className "button small alert"} "Cancelar")))))))

(om/root
 collection-modal-view
 state/app
 {:target (. js/document (getElementById "new-collection-modal"))})


(defn document-modal-view [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (dom/p #js {:className "lead"} "Introduce el documento (JSON)")
               (dom/form nil
                         (dom/textarea nil))
               (dom/a #js {:dangerouslySetInnerHTML #js {:__html "&#215;"}
                           :className "close-reveal-modal"} nil)
               (dom/ul #js {:className "button-group"}
                       (dom/li nil (dom/a #js {:className "button small"} "Aceptar"))
                       (dom/li nil (dom/a #js {:className "button small alert"} "Cancelar")))))))

(om/root
 document-modal-view
 state/app
 {:target (. js/document (getElementById "new-document-modal"))})


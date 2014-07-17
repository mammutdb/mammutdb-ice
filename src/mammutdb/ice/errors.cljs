(ns mammutdb.ice.errors
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [chan <! >! put! pub sub unsub unsub-all]]
            [cats.monad.maybe :as maybe]
            [cats.core :as m]
            [cats.monad.either :as either]
            [mammutdb.ice.parser :as p]
            [mammutdb.ice.modals :as modals]
            [mammutdb.ice.state :as state]
            [mammutdb.ice.events :refer [event-bus event-publisher event-publication]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]]))

(defn error-panel-view [data owner]
  (reify
    om/IRender
    (render [this]
      (if (:error data)
        (dom/div #js {:data-alert ""
                      :className "alert-box alert"}
                 (dom/span nil (str "Error: " (:error data)))
                 (dom/a #js {:onClick (fn [e] (state/disable-error!))
                             :dangerouslySetInnerHTML #js {:__html "&times;"}
                             :className "close"}))
        (dom/div nil)))))

(om/root
 error-panel-view
 state/app
 {:target (. js/document (getElementById "error-panel"))})

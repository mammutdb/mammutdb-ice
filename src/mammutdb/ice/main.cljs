(ns mammutdb.ice.main
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(def app-state (atom {}))

(defn content-view [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/h1 nil "MammutDB ICE"))))


(om/root
  content-view
  app-state
  {:target (. js/document (getElementById "content"))})

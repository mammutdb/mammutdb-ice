(ns mammutdb.ice.main
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(def app-state
  (atom {:databases [{:name "great_databases"}, {:name "piweek_winners"}]
         :collections [{:name "functional_databases"}, {:name "nosql_databases"}, {:name "sql_databases"}]
         :items (apply vector (map #(hash-map :id % :rev (str "1111-1111-" %) :createdAt "2014-01-01 12:00") (range 1 10)))

         ; :databases []
         ; :collections []
         ; :items []
         :show-query true
         }))

;; DATABASE LIST
(defn database-view [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/option nil (:name data)))))

(defn database-select-view [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:className "database-container"}
               (dom/h4 nil "Databases")
               (dom/button #js {:className "tiny"} "Add New")
               (dom/button #js {:className "tiny"} "Delete current")
               (if (= (count (:databases data)) 0)
                 (dom/p nil "No databases found")
                 (apply dom/select nil
                        (om/build-all database-view (:databases data))))
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
              (dom/a #js {:className "collection-link" :href "#"} (:name data))
              (dom/a #js {:className "close-btn" :href "#"} "x")))))

(defn collection-list-view [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:className "collections-container"}
               (dom/h4 nil "Collections")
               (dom/button #js {:className "tiny"} "Add New")
               (if (= (count (:collections data)) 0)
                 (dom/p nil "No collections found")
                 (apply dom/ul #js {:className "side-nav collections"}
                        (om/build-all collection-view (:collections data))))))))

(om/root
  collection-list-view
  app-state
  {:target (. js/document (getElementById "collection-list-view"))})

;; ITEMS
(defn item-view [data owner]
  (letfn [(create-item [data label field]
            (dom/div nil
                     (dom/span #js {:className "label"} label)
                     (dom/span #js {:className "field"} (field data))))]
    (reify
      om/IRender
      (render [this]
        (dom/div #js {:className "col-item"}
                 (create-item data "ID" :id)
                 (create-item data "REV" :rev)
                 (create-item data "Created" :createdAt))))))

(defn items-list-view [data owner]
  (reify
    om/IRender
    (render [this]
      (if (= (count (:items data)) 0)
        (dom/p nil "No items found")
        (apply dom/div #js {:className "collection"}
               (om/build-all item-view (:items data)))))))

(om/root
  items-list-view
  app-state
  {:target (. js/document (getElementById "item-list-view"))})

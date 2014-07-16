(ns mammutdb.ice.state)

(def app
  (atom {:databases []
         :collections []
         :documents []
         :show-query false
         :displaying-document nil
         :editing-document nil
         }))


(defn add-to-collection-sorted! [collection-key database]
  (letfn [(sort-insert [coll x] (sort (conj coll x)))]
    (swap! app update-in [collection-key] sort-insert database)))

(defn replace-by-id [collection document]
  (let [indexed-collection (map-indexed vector collection)
        [index _] (first (filter #(= (:_id (second %)) (:_id document)) indexed-collection))]
    (if index
      (assoc collection index document)
      (conj collection document))))

(defn add-database! [database]
  (add-to-collection-sorted! :databases database))

(defn add-collection! [collection]
  (add-to-collection-sorted! :collections collection))

(defn add-document! [document]
  (swap! app update-in [:documents] replace-by-id document)
  (swap! app assoc :displaying-document (:_id document)))

(defn displaying-document! [document-id]
  (swap! app assoc :displaying-document document-id))

(defn edit-document! [document-id data-text]
  (swap! app assoc :editing-document {:id document-id :data data-text}))

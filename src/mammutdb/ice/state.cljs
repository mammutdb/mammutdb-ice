(ns mammutdb.ice.state)

(def app
  (atom {:databases []
         :collections []
         :documents []
         :show-query false
         :displaying-document nil
         :displaying-revs []
         :editing-document nil
         :error nil
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

(defn set-revs! [revs]
  (swap! app assoc :displaying-revs revs))

(defn edit-document! [document-id data-text]
  (swap! app assoc :editing-document {:id document-id :data data-text}))

(defn set-error! [error]
  (swap! app assoc :error error))

(defn disable-error! []
  (swap! app dissoc :error))

(defn remove-by-id [map collection-key item-id id-key]
  (let [result (collection-key map)
        filter-fun (fn [item] (not= (id-key item) item-id))
        result (filter filter-fun result)
        result (into [] result)]
    (assoc map collection-key result)))

(defn remove-database! [database-id]
  (swap! app remove-by-id :databases database-id :id)
  (if (= (:selected-database @app) database-id)
    (swap! app dissoc :selected-database)))

(defn remove-collection! [collection-id]
  (swap! app remove-by-id :collections collection-id :id)
  (if (= (:selected-collection @app) collection-id)
    (swap! app dissoc :selected-collection)))

(defn remove-document! [document-id]
  (swap! app remove-by-id :documents document-id :_id))

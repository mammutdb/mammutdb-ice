(ns mammutdb.ice.state)

(def app
  (atom {:databases []
         :collections []
         :documents []
         :show-query false
         :displaying-document nil
         }))


(defn add-to-collection-sorted! [collection-key database]
  (letfn [(sort-insert [coll x] (sort (conj coll x)))]
    (swap! app update-in [collection-key] sort-insert database)))

(defn add-database! [database]
  (add-to-collection-sorted! :databases database))

(defn add-collection! [collection]
  (add-to-collection-sorted! :collections collection))

(defn add-document! [document]
  (swap! app update-in [:documents] conj document)
  (swap! app assoc :displaying-document (:_id document)))

(defn displaying-document! [document-id]
  (swap! app assoc :displaying-document document-id))

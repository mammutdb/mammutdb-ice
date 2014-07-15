(ns mammutdb.ice.state)

(def app
  (atom {:databases []
         :collections []
         :documents []
         :show-query false
         }))


(defn add-to-collection! [collection-key database]
  (letfn [(sort-insert [coll x] (sort (conj coll x)))]
    (swap! app update-in [collection-key] sort-insert database)))

(defn add-database! [database]
  (add-to-collection! :databases database))

(defn add-collection! [collection]
  (add-to-collection! :collections collection))

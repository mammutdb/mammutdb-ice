(ns mammutdb.ice.state)

(def app
  (atom {:databases []
         :collections []
         :documents []
         :show-query false
         }))

(defn add-database! [database]
  (letfn [(sort-insert [coll x] (sort (conj coll x)))]
    (swap! app update-in [:databases] sort-insert database)))

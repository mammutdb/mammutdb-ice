(ns mammutdb.ice.state)

(def app
  (atom {:databases []
         :collections []
         :documents []
         :show-query false
         }))

; TODO Should be a way to implement this better
(defn add-database! [database]
  (swap! app update-in [:databases] conj database)
  (swap! app update-in [:databases] sort))

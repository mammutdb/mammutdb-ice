(ns mammutdb.ice.state)

(def app
  (atom {:databases []
         :collections []
         :items []
         :show-query false
         }))

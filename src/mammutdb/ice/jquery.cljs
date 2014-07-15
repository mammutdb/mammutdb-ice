(ns mammutdb.ice.jquery)

(def jquery (js* "$"))

(defn close-modal [modal-id]
  (-> (jquery (str "#" modal-id))
      (.foundation "reveal" "close")))

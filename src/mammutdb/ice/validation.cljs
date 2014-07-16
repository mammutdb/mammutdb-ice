(ns mammutdb.ice.validation
  (:require [schema.core :as s]
            [cats.monad.either :as either]
            [clojure.string :refer [join]]))

(defn valid-name? [val]
  (boolean (re-matches #"\w+" val)))

(defn valid-json? [val] true)

(def create-schemas
  {:database
   {:database (s/both s/Str (s/pred valid-name?))}

   :collection
   {:collection (s/both s/Str (s/pred valid-name?))
    :type s/Keyword}

   :document
   {:document (s/both s/Str (s/pred valid-json?))}})


(defn validate-create-data [type data]
  (try
    (let [schema (type create-schemas)]
      (either/right (s/validate schema data)))
    (catch js/Error e
      (let [fields (->> (.-data e)
                        (:value)
                        (keys)
                        (map name)
                        (join ","))]
        (either/left  (str "Not valid fields: " fields))))))

(ns mammutdb.ice.validation
  (:require [schema.core :as s]
            [cats.monad.either :as either]
            [clojure.string :refer [join]]))

(defn valid-name? [val]
  (boolean (re-matches #"\w+" val)))

(defn valid-json?
  "Determines if a string is a valid json object. Will return
   false if it is a primitive different from Object"
  [val]
  (try
    (let [obj (.parse js/JSON val)]
      (and (= (type obj) js/Object)
           (not= (type obj) js/Array)))
    (catch js/Error e false)))

(def create-schemas
  {:database
   {:database (s/both s/Str (s/pred valid-name?))}

   :collection
   {:collection (s/both s/Str (s/pred valid-name?))
    :type (s/both  s/Str (s/pred (partial = "json")))}

   :document
   {:document (s/both s/Str (s/pred valid-json?))}})


(defn validate-create-data [type data]
  (try
    (let [schema (type create-schemas)]
      (either/right (s/validate schema data)))
    (catch js/Error e
      (let [fields (->> (.-data e)
                        (:error)
                        (keys)
                        (map name)
                        (join ","))]
        (either/left  (str "Not valid fields: " fields))))))

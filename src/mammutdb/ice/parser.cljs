(ns mammutdb.ice.parser
  (:require [schema.core :as s]
            [cats.monad.either :as either]
            [clojure.string :refer [join]]))


(defn txt->json [text]
  (try
    (either/right (.parse js/JSON text))
    (catch js/Error e
      (either/left (.-message e)))))

(defn json->txt [json]
  (try
    (either/right (.stringify js/JSON text))
    (catch js/Error e
      (either/left (.-message e)))))

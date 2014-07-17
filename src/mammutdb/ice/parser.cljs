(ns mammutdb.ice.parser
  (:require [schema.core :as s]
            [cats.core :as m]
            [cats.monad.either :as either]
            [cats.monad.maybe :as maybe]
            [clojure.string :refer [join]])
  (:import [java.lang.Integer]))


(defn txt->json [text]
  (try
    (either/right (.parse js/JSON text))
    (catch js/Error e
      (either/left (.-message e)))))

(defn json->txt [json]
  (try
    (either/right (.stringify js/JSON json))
    (catch js/Error e
      (either/left (.-message e)))))

(defn txt->clj [text]
  (m/>>= (txt->json text)
         (fn [json] (either/right (js->clj json)))))

(defn clj->txt [data]
  (json->txt (clj->js data)))

(defn parse-int [s]
  (let [result (js/parseInt s)]
    (if (js/isNaN result) nil result)))

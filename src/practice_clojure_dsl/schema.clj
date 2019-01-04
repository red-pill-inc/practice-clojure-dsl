(ns practice-clojure-dsl.schema
  (:import (clojure.lang Keyword PersistentHashSet PersistentVector IPersistentCollection AMapEntry PersistentArrayMap))
  (:require [clojure.spec.alpha :as s]
            [clojure.core.match :refer [match]]))

(s/def ::coll-of (s/or :maps (s/coll-of map?)
                       :kws (s/coll-of keyword?)
                       :numbers (s/coll-of number?)
                       :strings (s/coll-of string?)))

(defn- coll-type-of
  [v]
  (match (s/conform ::coll-of v)
         [:maps _] :db.type/ref
         [:kws _] :db.type/ref
         [:numbers _] :db.type/long
         [:strings _] :db.type/string
         :else "Invalid v"))

(comment
  (coll-type-of [0]))

(defn- value-type-of
  [v]
  (get {Long :db.type/long
        String :db.type/string
        Boolean :db.type/boolean
        Keyword :db.type/ref
        PersistentArrayMap :db.type/ref}
       (type v) (coll-type-of v)))

(defn- cardinality-of
  [v]
  (get {PersistentHashSet :db.cardinality/many
        PersistentVector :db.cardinality/many}
       (type v) :db.cardinality/one))

(defprotocol Schemify
  (schemify [data schema]))

(extend-protocol Schemify
  IPersistentCollection
  (schemify
    [data schema]
    (mapcat #(schemify % schema) data))
  AMapEntry
  (schemify
    [[k v] schema]
    (conj
      (schemify v schema)
      #:db{:ident k
           :valueType (value-type-of v)
           :cardinality (cardinality-of v)}))
  Keyword
  (schemify
    [kw schema]
    (conj schema #:db{:ident kw}))
  Object
  (schemify
    [_ schema]
    schema))

(comment
  (schemify {} [])
  (schemify :a [])
  (schemify [:a 0] [])
  (schemify {:a 0} [])
  (schemify {:a {:b 0}} [])
  (schemify {:a #{:b}} [])
  (schemify {:a [:b]} [])
  (schemify (first {:a [:b]}) [])
  (schemify {:a [0 1]} []))

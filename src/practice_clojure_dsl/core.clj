(ns practice-clojure-dsl.core
  (:require [datomic.api :as d]
            [practice-clojure-dsl.schema :refer [schemify]]
            [practice-clojure-dsl.constructs :refer :all]))

(defn fresh-database
  []
  (let [db-name (gensym)
        db-uri (str "datomic:mem://" db-name)]
    (d/create-database db-uri)
    (let [conn (d/connect db-uri)]
      ;@(d/transact conn construct-schema)
      conn)))

(def conn (fresh-database))

(comment
  (d/q '[:find ?n
         :where
         [_ :db/ident ?n]]
       (d/db conn))

  (let [type-a {:var/name :a
                :var/sort :type}
        type-b {:var/name :b
                :var/sort :type}
        prod-kind {:prod/params [type-a type-b]}
        prod-type {:prod/params [:bool :bool]}
        prod-member {:prod/params [:true :false]}]
    (:tx-data @(d/transact conn [{:rel/eq [prod-type prod-type]
                                  :eq/in [prod-kind]}
                                 {:rel/eq [prod-member prod-member]
                                  :eq/in [prod-type]}])))

  (let [spec1 {:spec/id "s1"}
        spec2 {:spec/id "s2"}
        spec3 {:spec/id "s3"
               :spec/input [spec1]
               :spec/output [spec2]}
        practice1 {:practice/id "p1"
                   :practice/specs [spec3]}
        place1 {:place/id "a1"
                :place/spec spec1}
        place2 {:place/id "a2"
                :place/spec spec2}
        operation1 {:op/id "op1"
                    :op/spec spec3
                    :op/input [place1]
                    :op/output [place2]
                    :op/body {}}
        resource1 {:res/id "res1"
                   :res/spec spec1}
        placing1 {:placing/id "pl1"
                  :placing/place place1
                  :placing/resource resource1}
        app1 {:app/id "app1"
              :app/op operation1
              :app/input [placing1]}
        work {:work/id "w1"
              :work/practice practice1
              :work/todo [app1]}]
    (:tx-data @(d/transact conn [work])))

  ; TODO:
  ; 1. спека для ресурсов
  ; 2. мутное назначение практики

  (let [value1 {:value/id "true"}
        value2 {:value/id "false"}
        type1 {:type/id "bool"
               :type/patterns [value1 value2]}
        type2 {:type/id "resource2-type"}
        type3 {:type/id "operation1-type"
               :type/from [type1]
               :type/to [type2]}
        type4 {:type/id "practice1-type"
               :type/types [type3]}
        place1 {:place/id "a1"}
        place2 {:place/id "a2"}
        resource1 {:resource/id "res1"}
        operation1 {:operation/id "op1"
                    :operation/from [place1]
                    :operation/to [place2]
                    :operation/body {}}
        specing1 {:specing/type type1
                  :specing/places [place1]
                  :specing/members [resource1]}
        specing2 {:specing/type type2
                  :specing/places [place2]}
        specing3 {:specing/type type3
                  :specing/members [operation1]}
        placing1 {:placing/place place1
                  :placing/resource resource1}
        todo1 {:todo/id "todo1"
               :todo/operation operation1
               :todo/from [placing1]}
        work1 {:work/id "w1"
               :work/practice type4
               :work/todos [todo1]}]
    (:tx-data @(d/transact conn [specing1
                                 specing2
                                 specing3
                                 work1])))

  (let [value1 {:value/id "true"
                :value/sort :value/value
                :value/shape :true}
        value2 {:value/id "false"
                :value/sort :value/value
                :value/shape :false}
        type1 {:value/id "bool"
               :value/sort :value/type-simple
               :value/shape :bool
               :value/patterns [value1 value2]}
        type2 {:value/id "resource2-type"}
        type3 {:value/id "operation1-type"
               :value/sort :value/type-operation
               :value/from [type1]
               :value/to [type2]}
        type4 {:value/id "practice1-type"
               :value/sort :value/type-interface
               :value/values [type3]}
        place1 {:value/id "a1"
                :value/sort :value/place}
        place2 {:value/id "a2"
                :value/sort :value/place}
        resource1 {:resource/id "res1"
                   :value/sort :value/value}
        operation1 {:operation/id "op1"
                    :operation/from [place1]
                    :operation/to [place2]}
                    ;:operation/body {}}
        specing1 {:specing/type type1
                  :specing/places [place1]
                  :specing/members [resource1]}
        specing2 {:specing/type type2
                  :specing/places [place2]}
        specing3 {:specing/type type3
                  :specing/members [operation1]}
        placing1 {:placing/id "placing1"
                  :placing/place place1
                  :placing/resource resource1}
        todo1 {:todo/id "todo1"
               :todo/operation operation1
               :todo/from [placing1]}
        work1 {:work/id "w1"
               :work/practice type4
               :work/todos [todo1]}
        schema (concat
                 (schemify specing1 [])
                 (schemify specing2 [])
                 (schemify specing3 [])
                 (schemify work1 []))]
    (do
      (:tx-data @(d/transact conn schema))
      (:tx-data @(d/transact conn [work1]))))

  (d/q '[:find (pull ?e [*])
         :where
         [?e :db/ident :value/from]]
       (d/db conn))

  (d/q '[:find ?n
         :where
         [_ :db/ident ?n]]
       (d/db conn))

  (d/q '[:find (pull ?e [*])
         :where
         [?e :value/id "bool"]]
       (d/db conn)))
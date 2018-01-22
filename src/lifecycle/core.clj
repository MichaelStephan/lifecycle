(ns lifecycle.core
  (:require [clojure.set :as s]))

(def tas-changelog [{:meta {:id "hana"
                            :ver 1
                            :gen 1}
                     :data {:marketing {}
                            :variants {}
                            :metrics {}}}])

(defn append-to-changelog [change-log id-obj data]
  (conj change-log (merge (update id-obj :ver inc) {:data data})))

(defn change-type [cmd location]
  (if (empty? (s/intersection (apply hash-set location) #{:variants}))
    :non-breaking
    :breaking))

(defn change [change-log {:keys [id ver] :as obj-id} changes options]
  (let [[[cmd location data :as change] & rest] changes]
    (if change
      (let [existing-obj (filter (fn [{{existing-meta-id :id
                                        existing-meta-ver :ver} :meta}]
                                   (and (= existing-meta-id id)
                                        (= existing-meta-ver ver)))
                                 change-log)
            change-type (change-type cmd location)]
        (if (or (contains? options :ignore-breaking-change)
                (= :non-breaking change-type))  
          (recur (condp = cmd
                   :update (append-to-changelog change-log obj-id (assoc-in {} location data))
                   :delete (append-to-changelog change-log obj-id (assoc-in {} location nil)))
                 obj-id rest options)
          {:error {:type :breaking-change-violation}}))
      change-log)))

(->
 (change tas-changelog {:id "hana" :ver 1}
         [[:update [:marketing :name] "hugo"]
         [:update [:marketing :description] "this is hugo"]
         [:update [:variants] 2]
         [:delete [:marketing :name]]]
         #{:ignore-breaking-change})
 (clojure.pprint/pprint))


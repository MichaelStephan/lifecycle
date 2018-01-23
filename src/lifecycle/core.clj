(ns lifecycle.core
  (:require [clojure.set :as s]))

(defn append-to-changelog [changelog entity-id data]
  (conj changelog (merge entity-id {:data data})))

(defn change-type [cmd location]
  (if (empty? (s/intersection (apply hash-set location) #{:variants :metrics}))
    :non-breaking
    :breaking))

(defn get-from-changelog [changelog {id-a :id major-a :major minor-a :minor :or {major 0 minor 0}}]
  (filter (fn [{id-b :id major-b :major minor-b :minor}]
            (and (= id-a id-b)
                 (if major-a (= major-a major-b) true)
                 (if minor-a (= minor-a minor-b) true)))
          changelog))

(defn exists-in-changelog? [changelog entity-id]
  (not (empty? (get-from-changelog changelog entity-id))))

(defn increment-minor [entity-id]
  (update entity-id :minor inc))

(defn increment-major-and-minor [entity-id]
  (update (increment-minor entity-id) :major inc))

(defn change [changelog entity-id [cmd location data :as change] & options]
  (let [entity-id (merge {:major 1 :minor 0} entity-id)
        new-entity-id (condp = (change-type cmd location)
                        :non-breaking (increment-minor entity-id)
                        :breaking (increment-major-and-minor entity-id))]
    (if (exists-in-changelog? changelog new-entity-id)
      (throw (ex-info "optimistic lock violation" {}))
      (append-to-changelog changelog new-entity-id (assoc-in {} location (condp = cmd
                                                                           :update data
                                                                           :delete nil))))))
(-> []
    (change {:id "hana"} [:update [:name] "hugo"])
    (change {:id "hana" :minor 1} [:update [:name] "hugo2"])
    (clojure.pprint/pprint))

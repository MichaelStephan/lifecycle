(ns lifecycle.core-test
  (:require [clojure.test :refer :all]
            [lifecycle.core :refer :all]))

(deftest test-get-from-changelog
  (let [not-existing-entity-id {:id 0 :major 0 :minor 0}
        existing-entity-id {:id "hana" :major 1 :minor 1}
        change-log [{:id "hana" :major 1 :minor 1 :data {:a 1}}
                    {:id "hana" :major 1 :minor 2 :data {:a 2}}
                    {:id "hana" :major 2 :minor 1 :data {:a 2 :b 1}}
                    {:id "hana" :major 2 :minor 2 :data {:a 2 :b 2}}
                    {:id "redis" :major 1 :minor 1 :data {:a 1}}]]
    (testing "try to get not-existing entry from an empty changelog"
      (is (empty? (get-from-changelog [] not-existing-entity-id))))
    (testing "try to get not-existing entry from a changelog"
      (is (empty? (get-from-changelog change-log not-existing-entity-id))))
    (testing "get (exact match) an existing entry from a changelog"
      (let [ret (get-from-changelog change-log existing-entity-id)]
        (is (= 1 (count ret)))
        (is (= (first ret) (first change-log)))))
    (testing "get (match only id) an existing entry from a changelog"
      (let [ret (get-from-changelog change-log (select-keys existing-entity-id [:id]))]
        (is (= 4 (count ret)))
        (is (= ret (take 4 change-log)))))
    (testing "get (match id and major version) an existing entry from a changelog"
      (let [ret (get-from-changelog change-log (dissoc existing-entity-id :minor))]
        (is (= 2 (count ret)))))
    (testing "get (match id and minor version) an existing entry from a changelog"
      (let [ret (get-from-changelog change-log (dissoc existing-entity-id :major))]
        (is (= 2 (count ret)))))))

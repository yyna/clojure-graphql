(ns clojure-graphql.schema
  "Contains custom resolvers and a function to provide the full schema."
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [com.walmartlabs.lacinia.schema :as schema]
    [com.walmartlabs.lacinia.util :as util]))

(defn resolve-game-by-id
  [games-map context args value]
  (let [{:keys [id]} args]
    (get games-map id)))

(defn resolver-map
  []
  (let [data (-> (io/resource "data.edn")
                 slurp
                 edn/read-string)
        games-map (->> data
                       :games
                       (reduce #(assoc %1 (:id %2) %2) {}))]
    {:query/game-by-id (partial resolve-game-by-id games-map)}))

(defn load-schema
  []
  (-> (io/resource "schema.edn")
      slurp
      edn/read-string
      (util/attach-resolvers (resolver-map))
      schema/compile))
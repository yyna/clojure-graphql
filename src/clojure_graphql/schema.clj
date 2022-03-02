(ns clojure-graphql.schema
  "Contains custom resolvers and a function to provide the full schema."
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [com.walmartlabs.lacinia.schema :as schema]
    [com.walmartlabs.lacinia.util :as util]))

(defn resolve-element-by-id
  [element-map context args value]
  (let [{:keys [id]} args]
    (get element-map id)))

(defn resolve-game-by-id
  [games-map context args value]
  (let [{:keys [id]} args]
    (get games-map id)))

(defn resolve-board-game-designers
  [designers-map context args board-game]
  (->> board-game
       :designers
       (map designers-map)))

(defn resolve-designer-games
  [games-map context args designer]
  (let [{:keys [id]} designer]
    (->> games-map
         vals
         (filter #(-> % :designers (contains? id))))))

(defn entity-map
  [data k]
  (reduce #(assoc %1 (:id %2) %2)
          {}
          (get data k)))

(defn rating-summary
  [data]
  (fn [_ _ board-game]
    (let [id (:id board-game)
          ratings (->> data
                       :ratings
                       (filter #(= id (:game_id %)))
                       (map :rating))
          n (count ratings)]
      {:count n
       :average (if (zero? n)
                  0
                  (/ (apply + ratings)
                     (float n)))})))

(defn member-ratings
  [ratings-map]
  (fn [_ _ member]
    (let [id (:id member)]
      (filter #(= id (:member_id %)) ratings-map))))

(defn game-rating->game
  [games-map]
  (fn [_ _ game-rating]
    (get games-map (:game_id game-rating))))

(defn resolver-map
  []
  (let [data (-> (io/resource "data.edn")
                 slurp
                 edn/read-string)
        games-map (entity-map data :games)
        members-map (entity-map data :members)
        designers-map (entity-map data :designers)]
    {:query/game-by-id (partial resolve-game-by-id games-map)
     :query/member-by-id (partial resolve-element-by-id members-map)
     :BoardGame/designers (partial resolve-board-game-designers designers-map)
     :BoardGame/rating-summary (rating-summary data)
     :GameRating/game (game-rating->game games-map)
     :Designer/games (partial resolve-designer-games games-map)
     :Member/ratings (member-ratings (:ratings data))}))

(defn load-schema
  []
  (-> (io/resource "schema.edn")
      slurp
      edn/read-string
      (util/attach-resolvers (resolver-map))
      schema/compile))
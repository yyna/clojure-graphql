(ns clojure-graphql.core
  (:require [clojure-graphql.schema :as schema]
            [clojure.data.json :as json]
            [com.walmartlabs.lacinia :refer [execute]]
            [reitit.ring :as ring]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.util.request :refer [body-string]]))

(def schema (schema/load-schema))

(defn graphql-handler
  [request]
  (let [{:keys [query variables]} (-> (body-string request)
                                      (json/read-str :key-fn keyword))
        result (execute schema query variables nil)]
    {:status 200
     :body (json/write-str result)}))

(defn wrap-content-type
  [handler content-type]
  (fn [request]
    (let [response (handler request)]
      (assoc-in response [:headers "Content-Type"] content-type))))

(def reloadable-app
  (-> (ring/ring-handler
        (ring/router
          ["/graphql"
           {:post {:handler graphql-handler}}]))
      (wrap-content-type "application/json")
      wrap-reload))

(defn -main []
  (jetty/run-jetty #'reloadable-app {:port  3000 :join? false}))

(comment
  (-main))
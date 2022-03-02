(ns clojure-graphql.core
  (:require [clojure-graphql.schema :as schema]
            [clojure.data.json :as json]
            [com.walmartlabs.lacinia :refer [execute]]
            [reitit.ring :as ring]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.resource :as resource]
            [ring.util.request :refer [body-string]]))

(def schema (schema/load-schema))

(defn graphql-handler
  [request]
  (let [{:keys [query variables]} (-> (body-string request)
                                      (json/read-str :key-fn keyword))
        result (execute schema query variables nil)]
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body (json/write-str result)}))

(def reloadable-app
  (-> (ring/ring-handler
        (ring/router
          ["/graphql"
           {:post {:handler graphql-handler}}]))
      (resource/wrap-resource "static")
      wrap-reload))

(defn -main []
  (jetty/run-jetty #'reloadable-app {:port  3000 :join? false}))

(comment
  (-main))
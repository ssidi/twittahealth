(ns twittahealth.core
  (:require [clojure.data.json :as json]
            [clojure.pprint :refer :all]
            [clojure.xml :as xml]
            [twitter.api.restful :as tapi]
            [twitter.oauth :as toauth]
            [clojure.core.async :as async]
            [environ.core :refer [env]]
            [clojure.tools.logging :as log]
            ))

;creds for twitter-api calls - read from environment
(def my-creds (toauth/make-oauth-creds (env :twitter-api-key)
                (env :twitter-api-secret)
                (env :twitter-user-token)
                (env :twitter-user-secret)))

(defn health-check
  []
  (try
    (let [result (tapi/statuses-update :oauth-creds my-creds
                   :params {:status "#twittahealth3"})]
      (log/debug "twitter status update result: " result))
    (catch Exception e
      (log/error "Error creating tweet/status update (exception, result): ")
      (log/error e)
;      (log/error result)))
  )))


(defn -main
  "twitts #teittahealth"
  []
  (health-check))

(ns twittahealth.core
  (:require [clojure.data.json :as json]
            [clojure.pprint :refer :all]
            [clojure.xml :as xml]
            [twitter.api.restful :as tapi]
            [twitter.oauth :as toauth]
            [clojure.core.async :as async]
            [environ.core :refer [env]]
            [clojure.tools.logging :as log]
            )
  (:import
    (com.twitter.hbc ClientBuilder)
    (com.twitter.hbc.core Client Constants)
    (com.twitter.hbc.core.endpoint StatusesFilterEndpoint Location Location$Coordinate)
    (com.twitter.hbc.core.processor StringDelimitedProcessor)
    (com.twitter.hbc.httpclient.auth Authentication OAuth1)
    (java.util.concurrent BlockingQueue LinkedBlockingQueue))
  )

;creds for twitter-api calls - read from environment
(def my-creds (toauth/make-oauth-creds (env :twitta-health-api-key)
                (env :twitta-health-api-secret)
                (env :twitta-health-user-token)
                (env :twitta-health-user-secret)))

;prefix used to send heartbeat
(def health-tag-prefix "#twittahealth")

(defn send-heartbeat []
  (try
    (let [twitter-status (str health-tag-prefix " " (rand-int Integer/MAX_VALUE))
          result (tapi/statuses-update :oauth-creds my-creds
                   :params {:status twitter-status})]
      (log/info "Sent heartbeat: " twitter-status))
    (catch Exception e
      (log/error "Error creating tweet/status update for: " health-tag-prefix)
      (log/error e)
  )))

;tag used to match when listening for heartbeat
(def health-match-tag ["#twittahealth"])

;generate auth object - creds for hbc calls
(def consumer-key (env :twitter-api-key) )
(def consumer-secret (env :twitter-api-secret))
(def token (env :twitter-user-token))
(def secret (env :twitter-user-secret))
(def auth (OAuth1. consumer-key consumer-secret token secret))
;create streaming endpoint filter
(def endpoint (StatusesFilterEndpoint.))

(defn create-client []

  ;set up a queue to store messages in
  (def queue (LinkedBlockingQueue. 10000))

  ;create a list of all track terms from all campaigns and use for twitter listening
  (. endpoint trackTerms health-match-tag)

  ;build the client
  (def client (-> (ClientBuilder.)
                (. hosts (. Constants STREAM_HOST))
                (. endpoint endpoint)
                (. authentication auth)
                (. processor (StringDelimitedProcessor. queue))
                (. build)))
  )

(defn listen-for-heartbeat []
    (create-client)

    ;connect the client
    (. client connect)

    ;inifinite loop, take message off queue each iteration and process it
    (log/info "Listening for heartbeat tweets...")
    (loop []
      (let [data (json/read-str (. queue take) :key-fn clojure.core/keyword)]
        (log/info "Received heartbeat: " (:text data)))
        (recur))
    (println "done listening"))

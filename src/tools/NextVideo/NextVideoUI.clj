(ns tools.NextVideo.NextVideoUI
  (:use [ring.middleware.file :only [wrap-file]])
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.adapter.jetty :as jet]
            [ring.middleware.reload :as reload]
            [ring.middleware.params :as wrap]
            [ring.middleware.resource :as res]
            [ring.util.response :as resp]
            [clojure.java.shell :as sh]
            [clojure.string :as str]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [tools.NextVideo.NextVideoBL :as buslog]
            ))


;;
;; *******************************************************************************
;; Helper functions for debugging
;; NOTE: These are needed because *out* is used by Jetty
;;

(defonce DEBUG-BUFFER (atom []))
(defonce DEBUG-BUFFER-MAX (atom 50))
(defn clear-debug [] (reset! DEBUG-BUFFER []))
(defn show-debug [] 
  (prn "DEBUG OUTPUT:")
  (doall (map #(prn %2 %1) @DEBUG-BUFFER (iterate inc 1)))
  nil
  )
(defn DEBUG [msg & args]
 (let [args-str (reduce (fn [res it] (str res " " it)) "" args)
       msg (str msg args-str)
       buf-len (count @DEBUG-BUFFER)]
   (when (>= buf-len @DEBUG-BUFFER-MAX) (reset! DEBUG-BUFFER []))
   (reset! DEBUG-BUFFER (conj @DEBUG-BUFFER msg)))
  
  )


(defonce RESOURCE_ROOT (atom "/Users/mkersh/clojure/Shared/NextVideo/resources-dev/"))

;;
;; This is a local version of resp/resource-response
;; resp/resource-response is not always working for me because of classpath issues (when I share a REPL across projects)
;; So this version just slurps response in from a specific filepath
(defn resource-response [relpath]
  (slurp (str @RESOURCE_ROOT relpath))
  )

(defn get-JSON-response [func request]
  {:status  200
   :headers {"Content-Type" "application/json"}
   :body  (json/write-str (func request))}
  )

(defonce UID (atom nil))

;; get UID
(defn getUserID [request]
  (let [uid (get (:params request) "UID")
        uid (str/lower-case uid)]
    (DEBUG "getUserID" uid)
    (if (= @UID uid) uid
        (do
          (buslog/load-user-data uid)
          (reset! UID uid)))
    uid))

(defn get-all-series [request]
  (DEBUG "get-all-series called")
  (getUserID request)
  (buslog/list-all-series)
  )

;; :seriesID in :params
(defn get-series [request] 
  (DEBUG "get-series called")
  (getUserID request)
  (let [seriesID (:seriesID (:params request))]
    (DEBUG "seriesID=" seriesID)
    (buslog/get-next-episode seriesID))
  )

(defn get-watchlist [request]
  (DEBUG "get-watchlist called")
  (getUserID request)
  (buslog/list-carry-on-watchlist)
  )

(defn play-series [request]
  (DEBUG "play-series called")
  (getUserID request)
  (let [seriesID (:seriesID (:params request))]
    (DEBUG "seriesID=" seriesID)
    (buslog/play-episode-num @UID seriesID))) 

(defn inc-series [request]
  (DEBUG "inc-series called")
  (getUserID request)
  (let [seriesID (:seriesID (:params request))]
    (DEBUG "seriesID=" seriesID)
    (buslog/inc-episode-num @UID seriesID 1)
    (buslog/get-next-episode seriesID)
    ))

(defn dec-series [request]
  (DEBUG "dec-series called")
  (getUserID request)
  (let [seriesID (:seriesID (:params request))]
    (DEBUG "seriesID=" seriesID)
    (buslog/inc-episode-num @UID seriesID -1)
    (buslog/get-next-episode seriesID)
    ))

(defn inc-season [request] 
  (getUserID request)
  (let [seriesID (:seriesID (:params request))]
    (DEBUG "seriesID=" seriesID)
    (buslog/inc-episode-num @UID seriesID 100)
    (buslog/get-next-episode seriesID)))

(defn dec-season [request]
  (DEBUG "dec-season called")
  (getUserID request)
  (let [seriesID (:seriesID (:params request))]
    (DEBUG "seriesID=" seriesID)
    (buslog/inc-episode-num @UID seriesID -100)
    (buslog/get-next-episode seriesID)
    ))


;; *********************************
;; Define the possible routes of our webserver
;; 
;;
(defroutes app
  (GET "/" [] (resource-response "public/index.html"))
  ;; test route, not used by program
  (GET "/about/:id" request (str "<h1>AAAAAHello WorldAAAA!!!</h1>" (:id (:params request)) request)) 
  ;;(GET "/Series" request (get-JSON-response get-all-series request)) 
  (GET "/WatchList" request (get-JSON-response get-watchlist request))
  ;;(GET "/Series/:seriesID" request (get-JSON-response get-series request))
  (GET "/Series/:seriesID/Play" request (get-JSON-response play-series request))
  (GET "/Series/:seriesID/Inc" request (get-JSON-response inc-series request))
  (GET "/Series/:seriesID/Dec" request (get-JSON-response dec-series request))
  (GET "/Series/:seriesID/IncSeason" request (get-JSON-response inc-season request))
  (GET "/Series/:seriesID/DecSeason" request (get-JSON-response dec-season request)) 

  (route/not-found "<h1>Page not found</h1>"))

;; https://github.com/ring-clojure/ring/issues/104
(def app-with-reload
  ;; Using two middleware handlers here
  (res/wrap-resource (wrap/wrap-params (reload/wrap-reload #'app)) "public"))

(defonce server (jet/run-jetty #'app-with-reload {:join? false :port 3002}))

(defn start-bookmark-server [] (.start server))
(defn stop-bookmarkserver [] (.stop server))


(comment
  ;; *********************************************
  ;; [1] Start/Stop the webserver

  (start-bookmark-server) ;; http://localhost:3002
  (stop-bookmarkserver)
  
  (show-debug)
  (clear-debug)

;;
  )



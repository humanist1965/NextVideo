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
             [clojure.java.shell :as sh]
            [cemerick.pomegranate :as pom]
            [clojure.java.shell :as sh]
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

;;
;; Not using the local version of resource-response anymore.
;;
(defonce RESOURCE_ROOT (atom "/Users/mkersh/clojure/Shared/NextVideo/resources-dev/"))

(defn resource-path [relpath]
 (str @RESOURCE_ROOT relpath))

;;
;; This is a local version of resp/resource-response
;; resp/resource-response is not always working for me because of classpath issues (when I share a REPL across projects)
;; So this version just slurps response in from a specific filepath
(defn resource-response [relpath]
  (slurp (resource-path relpath))
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
  (DEBUG "get-watchlist called xxx")
  (let [uid (getUserID request)
        _ (buslog/load-user-data uid)
        watchlist (buslog/list-carry-on-watchlist)
        cur-obj (first watchlist)
        seriesID (when cur-obj (:seriesID cur-obj))
        url (when cur-obj (:url cur-obj))]
    (if url watchlist 
        ;; if URL is not defined then using the following kludge 
        (let [_ (buslog/get-next-episode seriesID) 
              _ (buslog/save-user-data uid)
              _ (buslog/load-user-data uid)
              watchlist (buslog/list-carry-on-watchlist)
              cur-obj (first watchlist) 
              url (when cur-obj (:url cur-obj))
              _ (DEBUG "get-watchlist finding URL called xxx" url cur-obj)]
          watchlist))
    watchlist)
  )

(defn play-series [request]
  (DEBUG "play-series called")
  (getUserID request)
  (let [seriesID (:seriesID (:params request))]
    (DEBUG "seriesID=" seriesID)
    (buslog/play-episode-num @UID seriesID)
    (buslog/inc-episode-num @UID seriesID 1)
    )) 

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

(defn printClassPath []
  (DEBUG "JAVA CLASSPATH - Has the following configuration:")
  (DEBUG "===============")
  (DEBUG (sort (str/split
                    (System/getProperty "java.class.path")
                    #":"))))

(defn get-debug [_request] 
  ;;(DEBUG "cd:" (sh/sh "cd" "/Users/mkersh/clojure/Shared/NextVideo/resources-dev/"))
  ;;(DEBUG "PWD:" (sh/sh "pwd"))
  ;;(DEBUG "CLASSPATH=" (printClassPath))
  @DEBUG-BUFFER
  )

;;
;; handler for diabling caching on all pages
;; NOTE: Does not appear to help with my current issue
;;
(defn wrap-nocache [handler]
  (fn [request] (-> request
                    handler
                    (assoc-in [:headers "Pragma"] "no-cache"))))


(defn wrap-cache-buster
  "Prevents any and all HTTP caching by adding a Cache-Control header
  that marks contents as private and non-cacheable."
  [handler]
  (fn wrap-cache-buster-handler [response]
    (resp/header (handler response) "cache-control" "private, max-age=0, no-cache")))

;; *********************************
;; Define the possible routes of our webserver
;; 
;;
(defroutes app
  (GET "/" [] (resp/resource-response "public/index.html"))
  ;;(GET "/" [] (resource-response "public/index.html"))
  ;; test route, not used by program
  (GET "/about/:id" request (str "<h1>AAAAAHello WorldAAAA!!!</h1>" (:id (:params request)) request))
  ;;(GET "/Series" request (get-JSON-response get-all-series request)) 
  (GET "/showdebug" request (get-JSON-response get-debug request))
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
  (wrap-cache-buster (res/wrap-resource (wrap/wrap-params (reload/wrap-reload #'app)) "public")))


(defonce WEBSERVER (atom nil))
(defn server [] 
  (if @WEBSERVER
    @WEBSERVER
    (let [ws (jet/run-jetty #'app-with-reload {:join? false :port 8000})
          _ (reset! WEBSERVER ws)]
      ws)))

(defn load-resource
  [name]
  (let [rsc-name (str name)
        thr (Thread/currentThread)
        ldr (.getContextClassLoader thr)]
    (.getResourceAsStream ldr rsc-name)))

(defn get-environment-variable [var-name]
  (System/getenv var-name))

(defn set-environment-variable [var-name var-value]
  (System/setProperty var-name var-value))

(defn extend-resource-path []
  (let [nv-res-path  (get-environment-variable "NV_RES_PATH")]
    (when nv-res-path  (pom/add-classpath nv-res-path))))

(defn start-bookmark-server [] (extend-resource-path)
  (printClassPath)
  (.start (server)))
(defn stop-bookmarkserver [] (.stop (server)))



(comment
  ;; *********************************************
  ;; [1] Start/Stop the webserver
  (start-bookmark-server) ;; http://localhost:8000
  (stop-bookmarkserver)
  @WEBSERVER
  (server)
  (show-debug)
  (clear-debug)
  @RESOURCE_ROOT
  (resource-response "public/index.html")
  (resource-path "public")
  (resource-path "public/index.html")


  ; Usage example 
  (def file-contents (load-resource "mk.txt"))
  (def file-contents (load-resource "public/index3.html"))
  (pom/add-classpath "/Users/mkersh/tmptt")
  (slurp file-contents) 
  (load-resource "mk.txt")
  (println file-contents)

  (get-environment-variable "NV_RES_PATH")
  (set-environment-variable "NV_RES_PATH" "/Users/mkersh/tmptt")
  (extend-resource-path)
  (sh/sh "env")
  
  
  ;;
  )



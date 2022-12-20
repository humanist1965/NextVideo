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
          )   

)


(defonce DEBUG-BUFFER (atom []))
(defn clear-debug [] (reset! DEBUG-BUFFER []))
(defn show-debug [] @DEBUG-BUFFER)
(defn DEBUG [msg]
  (reset! DEBUG-BUFFER (conj @DEBUG-BUFFER msg)))


(defonce RESOURCE_ROOT (atom "/Users/mkersh/clojure/Shared/NextVideo/resources-dev/"))

;;
;; This is a local version of resp/resource-response
;; resp/resource-response is not always working for me because of classpath issues (when I share a REPL across projects)
;; So this version just slurps response in from a specific filepath
(defn resource-response [relpath]
  (slurp (str @RESOURCE_ROOT relpath))
  )

(show-debug)
(clear-debug)
;; *********************************
;; Define the possible routes of our webserver
;; 
;;
(defroutes app
  (GET "/" [] (resource-response "public/index.html")) 
  (GET "/about" request (str "<h1>AAAAAHello WorldAAAA!!!</h1>" request))
  (GET "/withdraw" request {:status  200
                            :headers {"Content-Type" "application/html"}
                            :body  "<h1>Withdraw</h1>"})
  (GET "/check" request {:status  200
                         :headers {"Content-Type" "application/html"}
                         :body  "<h1>Withdraw</h1>"})
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

;; Not picking up main.js

https://stackoverflow.com/questions/12336643/how-to-serve-video-files-to-the-ipad-using-jetty-ring
  https://ring-clojure.github.io/ring/ring.middleware.file.html
  
;;
  )



(ns tools.NextVideo.NextVideoUI
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


(show-debug)
(clear-debug)
;; *********************************
;; Define the possible routes of our webserver
;; 
;;
(defroutes app
  (GET "/" [] (resp/resource-response "public/main.html")) 
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

(resp/resource-response "public/nxtui/main.html")
  
  (io/resource "/Users/mkersh/clojure/Shared/NextVideo/resources-dev/public/main.html")
    (slurp "/Users/mkersh/clojure/Shared/NextVideo/resources-dev/public/main.html")

  
  
;;
  )



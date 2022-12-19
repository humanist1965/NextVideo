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
          [clojure.data.json :as json])

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
  (GET "/" [] (resp/resource-response "public/fees-poc.html")) ; #search= dfbab3ef-0544-4037-bc6b-ebafe0186efc 
  (GET "/about" request (str "<h1>Hello World!!!</h1>" request))
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


;;
  )



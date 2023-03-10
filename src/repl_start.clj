(ns repl_start
  (:require [tools.NextVideo.NextVideoUI :as nv])
  (:gen-class))

;; This is the main function that will get called when you start using:
;; lein run
;; NOTE: The :main tag in project.clj determines what namespace/file to start in and will look for 
;; a -main function in here. The project.clj defines ":main repl_start" and so the -main below
;; is the startup function for us.
;;
(defn -main []
    (nv/DEBUG "Starting up NextVideo webserver") 
    (nv/start-bookmark-server) ;; http://localhost:8000 http://localhost:8000/showdebug
)

;;(defonce start-up  (-main))

(comment
  (-main)
  (def start-up  (-main))
  (nv/stop-bookmarkserver)
  (nv/show-debug)
  (nv/clear-debug)

  ;;
  )
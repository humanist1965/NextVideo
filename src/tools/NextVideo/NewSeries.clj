
(ns tools.NextVideo.NewSeries
  (:require [clojure.string :as str]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [tools.NextVideo.NV_datastore :as db]
            [clojure.pprint :as pp]
            [clj-yaml.core :as yaml]
            ))


(defn isHttpURL [url])

(defn _getHttpURL [url])

(defn _getStrFromFile [relFilePath])

(defn _extractVideoLinks [searchStr])

(defn _saveToYAMLFile [seriesID seasonNum outStr])

(defn createYAMLfromTVHOMEUrl [seriesID seasonNum url])



(defn main []
  (createYAMLfromTVHOMEUrl "WestWorld",1,"TVHOME_RAW/WestWorld1.html") 
  )

(comment
  (main)
  
  ;;
  )
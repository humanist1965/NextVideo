
(ns tools.NextVideo.NewSeries
  (:require [clojure.string :as str]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [tools.NextVideo.NV_datastore :as db]
            [clojure.pprint :as pp]
            [clj-yaml.core :as yaml]
            ))

(defn init [rootpath]
  (io/make-parents rootpath)
  (atom rootpath))



(defonce ROOTDIR (init "src/tools/NextVideo/"))
(defonce YAMLROOT (init (str @ROOTDIR "YAML_FILES/")))


(defn get-html-filepath [relpath]
  (str @ROOTDIR relpath)
  )

(defn get-yaml-filepath [relpath]
  (str @YAMLROOT relpath))

(defn isHttpURL [url]
  (str/includes? url "http:"))


(defn _getHttpURL [url]
  (assert false "ERROR: Not implemented yet!!")
  )

(defn _getStrFromFile [relFilePath] 
  (let [fpath (get-html-filepath relFilePath) 
        file-str (slurp fpath)]
    file-str))

(defn _extractVideoLinks [searchStr]
  (let [regExpStr #"href=\"/play.php.*\""]
    (re-seq regExpStr searchStr)
    )
  )

(defn _saveToYAMLFile [seriesID seasonNum outStr]
  (let [relpath (str seriesID seasonNum ".yml")
        filepath (get-yaml-filepath relpath)]
    (if (.exists (io/file filepath))
      (prn "Error file already exists - aborting!:" filepath)
      ;;(spit filepath outStr)
      (spit filepath outStr)))
  )

(defn enumerate [lst1]
  (mapv (fn [it i] [i it]) lst1 (iterate inc 1))
  )

(defn createYAMLfromTVHOMEUrl [seriesID seasonNum url]
  (let [urlData (if (isHttpURL url) (_getHttpURL url) (_getStrFromFile url))
        episodeList (_extractVideoLinks urlData)
        episodeList (map (fn [it]
                           (let [modIt (subs it 6)
                                 modIt (subs modIt 0 (dec (count modIt)))
                                 modIt (str "http://tvhome.cc" modIt)
                                 modIt (if (or (str/ends-with? modIt "|1")
                                               (str/ends-with? modIt "|2"))
                                         (subs modIt 0 (- (count modIt) 2))
                                         modIt)]
                             modIt)) episodeList)
        episodeList (rest (reverse episodeList)) ;; reverse and remove first
        outStr "---\n"
        outStr (reduce (fn [res [i it]]
                         (let [res (str res "-\n")
                               res (str res "  episode: " i "\n")
                               res (str res "  url: " it "\n")]
                           res)) 
                         outStr (enumerate episodeList))  
        ]
    (_saveToYAMLFile seriesID seasonNum outStr)))


(defn main2 []
    (createYAMLfromTVHOMEUrl "24L",1,"TVHOME_RAW/24L1.html")

  ;; (createYAMLfromTVHOMEUrl "MITB",1,"TVHOME_RAW/mitb1.html")
  ;; (createYAMLfromTVHOMEUrl "MITB",2,"TVHOME_RAW/mitb2.html")
  ;; (createYAMLfromTVHOMEUrl "MITB",3,"TVHOME_RAW/mitb3.html")
  )

(comment
  (main2) 

  (/ 3.0)
  ;;
  )
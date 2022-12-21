(ns tools.NextVideo.NV_datastore
  (:require [clojure.string :as str]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clj-yaml.core :as yaml]
            ))


(defn init [rootpath]
  (io/make-parents rootpath)
  (atom rootpath))

(defn opt-add-seperator [fpath]
  (let [str-len (count fpath)
        last-ch (get fpath (- str-len 1))]
    (if (= last-ch \/) fpath (str fpath "/"))))

(defonce DBROOT (init "src/tools/NextVideo/$$DATASTORE$$/"))
(defonce YAMLROOT (init "src/tools/NextVideo/YAML_FILES/"))

;; https://rosettacode.org/wiki/Walk_a_directory/Recursively#Clojure
(defn walk-dir [dirpath pattern]
  (doall (filter #(re-matches pattern (.getName %))
                 (file-seq (io/file dirpath)))))

(defn map-all-files 
  ([dirpath func](map-all-files dirpath func #".*\.*"))
  ([dirpath func pattern]
  (map func (walk-dir dirpath pattern))))


(defn walk-directory-recursive
  ([dirpath] (walk-directory-recursive (io/file dirpath) [] {}))
  ([dirpath options] (walk-directory-recursive (io/file dirpath) [] options))
  ([^java.io.File file result-list options] 
  (let [include-dirs (:include-dirs? options)
        top-level-only (:top-level-only? options)]
    (if (.isDirectory file)
      (let [result-list (if include-dirs (conj result-list file) result-list)]
        (reduce (fn [res file-in-dir] 
                  (if (and top-level-only (.isDirectory file-in-dir))
                    res ;; don't recurse into sub-directories
                    (walk-directory-recursive file-in-dir res options)))
                result-list  (.listFiles file))
        )
      (conj result-list file)))))

(defn yaml-file-to-edn [yaml-path]
  (let [yaml-str (slurp yaml-path)
        edn-obj (yaml/parse-string yaml-str)]
    edn-obj))

(defn getYAML [relPath]
  (let [fpath (str relPath ".yml")
        fpath (str @YAMLROOT fpath)
        fpath (opt-add-seperator fpath)]
    (yaml-file-to-edn fpath))
  )

(defn getSubKeys [relPath]
  (let [fpath (str @DBROOT relPath)
        fpath (opt-add-seperator fpath) 
        ]
    (walk-directory-recursive fpath {:include-dirs? false :top-level-only? false})
    (map-all-files fpath identity #".*\.json")
    )
  )

(defn convertJsonFileToEdn [fn]
  (let [fileStr (slurp fn)]
    (json/read-str fileStr)))

(defn getJSON [absPath]
;;    (let [fpath (str @DBROOT relPath)
;;          fpath (opt-add-seperator fpath)]
     (slurp absPath)
     
    ;; )
  
  )

(defn convert-to-keywords [obj]
  (reduce (fn [res [key1 it]]
            (let [keywd1 (keyword key1)]
              (assoc res keywd1 it)))
          {} obj))

(defn getObj [relPath]
  (let [json-str (getJSON relPath)
        obj (json/read-str json-str)
        obj (convert-to-keywords obj)
        ]
    obj))
  
  

(defn storeJSON [relPath obj]
  (let [json-str (json/write-str obj)
        fpath (str @DBROOT relPath)
        fpath (opt-add-seperator fpath) 
        fpath (str fpath "data.json") 
        _ (io/make-parents fpath)]
    (spit fpath json-str)))



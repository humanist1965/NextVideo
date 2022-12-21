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

;; https://rosettacode.org/wiki/Walk_a_directory/Recursively#Clojure
(defn walk-dir [dirpath pattern]
  (doall (filter #(re-matches pattern (.getName %))
                 (file-seq (io/file dirpath)))))

(defn map-all-files [dirpath func]
  (map func (walk-dir dirpath #".*\.*")))


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
  (let [fpath (str @DBROOT relPath)
        fpath (opt-add-seperator fpath)]
    (yaml-file-to-edn fpath))
  )

(defn getSubKeys [relPath]
  (let [fpath (str @DBROOT relPath)
        fpath (opt-add-seperator fpath) 
        ]
    (walk-directory-recursive fpath {:include-dirs? false :top-level-only? true}))
  )

(defn convertJsonFileToEdn [fn]
  (let [fileStr (slurp fn)]
    (json/read-str fileStr)))

(defn getJSON [relPath]
   (let [fpath (str @DBROOT relPath)
         fpath (opt-add-seperator fpath)]
     (slurp fpath))
  
  )

(defn getObj [relPath]
  (let [json-str (getJSON relPath)
        obj (json/read-str json-str)]
    obj))
  
  

(defn storeJSON [relPath obj]
  (let [json-str (json/write-str obj)
        fpath (str @DBROOT relPath)
        fpath (opt-add-seperator fpath)
        fpath (str fpath "data.json") 
        _ (io/make-parents fpath)]
    (spit fpath json-str)))



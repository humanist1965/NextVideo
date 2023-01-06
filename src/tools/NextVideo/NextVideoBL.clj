(ns tools.NextVideo.NextVideoBL
  (:require [clojure.string :as str]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [tools.NextVideo.NV_datastore :as db]
             [clojure.pprint :as pp]
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
  nil)
(defn DEBUG [msg & args]
  (let [args-str (reduce (fn [res it] (str res " " it)) "" args)
        msg (str msg args-str)
        buf-len (count @DEBUG-BUFFER)]
    (when (>= buf-len @DEBUG-BUFFER-MAX) (reset! DEBUG-BUFFER []))
    (reset! DEBUG-BUFFER (conj @DEBUG-BUFFER msg))))


(defn now-datetime []
  (.format (java.text.SimpleDateFormat. "yyyy-MM-dd HH.mm.ss") (new java.util.Date)))

(defonce WATCH_LIST_DICT (atom {}))
(defonce WATCH_LIST (atom []))
(defonce ALL_LIST (atom []))

(defn load-user-data [userID]
  (let [all-list (db/getYAML "AllSeriesList")
        all-dict (reduce (fn [res it]
                           (assoc res (get it :seriesID) it)) {} all-list)
        watchListKeys (db/getSubKeys userID)
        watchList (reduce (fn [res it]
                            (let [user-obj (db/getObj it)]
                              (conj res user-obj)))
                          [] watchListKeys)
       
        watchList (sort (fn [it1 it2]
                          (let [lwd1 (:lastWatchedDate it1)
                                lwd2 (:lastWatchedDate it2)]
                            (compare lwd2 lwd1)))
                        watchList)
        
        
        consideredList (reduce (fn [res it]
                                 (let [seriesID (get it :seriesID)]
                                   (assoc res seriesID true))) {} watchList)
        watchListExt (mapv (fn [obj]
                             (let [seriesID (get obj :seriesID)
                                   glob-obj (get all-dict seriesID)
                                   name (get glob-obj :name)
                                   image (get glob-obj :image)]
                               (merge obj {:name name :image image})))
                           watchList)
       
        ;; At the moment as part of MVP include any missing global items onto the user watchList
        ;; May change this in future to require users to add Series they want to watch
        ;;
        watchListExt (reduce (fn [res it]
                               (let [seriesID (get it :seriesID)
                                     alreadyIn? (get consideredList seriesID)]
                                 (if alreadyIn? res
                                     (conj res (merge it {:currentSeasonNumber 1 :nextEpisodeNumber 1 :lastWatchedDate "1900-01-01"})))))
                             watchListExt all-list) 
        
        watchListDict (reduce (fn [res it]
                                (let [seriesID (get it :seriesID)]
                                  (assoc res seriesID it))) {} watchListExt) 
        ]
    (reset! WATCH_LIST_DICT watchListDict)
    (reset! WATCH_LIST watchListExt)
    (reset! ALL_LIST all-list)
    (prn "load-user-data into @WATCH_LIST_DICT @WATCH_LIST @ALL_LIST")
    
    ))

(defn get-watch-list-dict [seriesID]
  (get @WATCH_LIST_DICT seriesID) 
  )

(defn save-user-data [userID]
  (doall (map (fn [[_ it]]
         (let [seriesID (get it :seriesID)
               key (str userID "/" seriesID)]
           (db/storeJSON key it)))
       @WATCH_LIST_DICT))
  (prn "save-user-data - finished")
  
  )

(defn update-user-data [seriesID attr val] 
  (let [watchlist @WATCH_LIST_DICT
        userObj (get watchlist seriesID)
        userObj (assoc userObj attr val) 
        watchlist (assoc watchlist seriesID userObj)
        ]
    (get (reset! WATCH_LIST_DICT watchlist) seriesID)
    )
  )


(defn loadSeasonData [seriesID curSeasonNum] 
   (let [key (str seriesID curSeasonNum)
         seasonData (try (db/getYAML key) (catch Exception _ nil))]
     seasonData
     )
  )

(defn get-episode-data [season-data curEpisodeNum]
  (let [curEpisodeNum (Integer/parseInt (str curEpisodeNum))
        maxLen (count season-data)
        invalid-num (not (and (<= curEpisodeNum maxLen) (> curEpisodeNum 0)))
        curEpisodeNum (- curEpisodeNum 1)]
    (if invalid-num nil (nth season-data curEpisodeNum))))


(defn list-all-series []
  @ALL_LIST
  )


(defn list-carry-on-watchlist []
  @WATCH_LIST)


(defn get-next-episode [seriesID]
  (let [user-obj (get-watch-list-dict seriesID)
        curSeasonNum (get user-obj :currentSeasonNumber)
        curEpisodeNum (get user-obj :nextEpisodeNumber)
        season-data (loadSeasonData seriesID curSeasonNum)
        episode-data (get-episode-data season-data curEpisodeNum)
        url (get episode-data :url)]
    (when url 
      (update-user-data seriesID :url url)
      (assoc user-obj :url url))))

(defn play-episode-num [userID seriesID]
  (let [dt-str (now-datetime)]
    (update-user-data seriesID :lastWatchedDate dt-str) 
    (save-user-data userID)
    ))

;;
;; next function can be used to inc/dec season and episode numbers
;; NOTE: To inc/dec season pass a large incNum
;;
(defn inc-episode-num 
  ([userID seriesID](inc-episode-num userID seriesID 1))
  ([userID seriesID incNum]
  (let [user-obj (get-watch-list-dict seriesID)
        curSeasonNum (get user-obj :currentSeasonNumber)
        curEpisodeNum (get user-obj :nextEpisodeNumber)
        curEpisodeNum (+ curEpisodeNum incNum)
        _ (update-user-data seriesID :nextEpisodeNumber curEpisodeNum)
        eps-obj (get-next-episode seriesID)]
    (if eps-obj (save-user-data userID)
        (let [seasonInc (if (< incNum 0) -1 1)
              curSeasonNum (+ curSeasonNum seasonInc)
              curSeasonNum (if (<= curSeasonNum 0) 1 curSeasonNum)
              season-data (loadSeasonData seriesID curSeasonNum)
              curSeasonNum (if season-data curSeasonNum 1)
              season-data (if season-data season-data (loadSeasonData seriesID curSeasonNum))
              season-len (count season-data)
              curEpisodeNum (if (< incNum 0) season-len 1)]
          (update-user-data seriesID :currentSeasonNumber curSeasonNum)
          (update-user-data seriesID :nextEpisodeNumber curEpisodeNum)
          (save-user-data userID))
        ))))


(comment

  ;; testing the above
  
  (now-datetime)
  (loadSeasonData "WIRE" 5)
  (play-episode-num "mark" "WIRE")
  (update-user-data "WIRE" :currentSeasonNumber 5)
  (update-user-data "WIRE" :nextEpisodeNumber 10)
  (for [_i (range 1)](inc-episode-num "mark" "WIRE" 1))
  (loadSeasonData "WIRE" 5)
  (load-user-data "mark")
  (get-next-episode "TWL")
  @WATCH_LIST_DICT
  (get @WATCH_LIST_DICT "TWL")
  @WATCH_LIST 
  @ALL_LIST
  (db/getYAML "AllSeriesList")
  (load-user-data "mark")
  
   (load-user-data "guest")
  (get @WATCH_LIST_DICT "TWL") 
 (get-next-episode "TWL")
 (save-user-data "guest")
 (load-user-data "guest")

  ;;
  )






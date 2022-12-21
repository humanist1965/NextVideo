(ns tools.NextVideo.NextVideoBL
  (:require [clojure.string :as str]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [tools.NextVideo.NV_datastore :as db]
            ))


(defn now-datetime []
  (.format (java.text.SimpleDateFormat. "yyyy-MM-dd HH.MM.SS") (new java.util.Date)))

(defonce WATCH_LIST_DICT (atom {}))
(defonce WATCH_LIST (atom []))
(defonce ALL_LIST (atom []))

(defn load-user-data [userID]
  (let [all-list (db/getYAML "AllSeriesList")
        all-dict (reduce (fn [res it]
                           (assoc res (get it "seriesID") it)) {} all-list)
        watchListKeys (db/getSubKeys userID)
        watchList (reduce (fn [res it]
                            (let [user-obj (db/getObj it)]
                              (conj res user-obj)))
                          [] watchListKeys)
        watchList (sort watchList)
        consideredList (reduce (fn [res it]
                                 (let [seriesID (get it "seriesID")]
                                   (assoc res seriesID true))) {} watchList)
        watchListExt (mapv (fn [obj]
                             (let [seriesID (get obj "seriesID")
                                   glob-obj (get all-dict seriesID)
                                   name (get glob-obj "name")
                                   image (get glob-obj "image")]
                               (merge obj {"name" name "image" image})))
                           watchList)

        ;; At the moment as part of MVP include any missing global items onto the user watchList
        ;; May change this in future to require users to add Series they want to watch
        ;;
        watchListExt (reduce (fn [res it]
                               (let [seriesID (get it "seriesID")
                                     alreadyIn? (get consideredList seriesID)]
                                 (if alreadyIn? res
                                     (conj res (merge it {"currentSeasonNumber" 1 "nextEpisodeNumber" 1 "lastWatchedDate" "1900-01-01"})))))
                             watchListExt all-list)

        watchListDict (reduce (fn [res it]
                                (let [seriesID (get it "seriesID")]
                                  (assoc res seriesID it))) {} watchListExt) 
        ]
    (reset! WATCH_LIST_DICT watchListDict)
    (reset! WATCH_LIST watchListExt)
    (reset! ALL_LIST all-list)
    {:all-list all-list :watchList watchListExt :watchListDict watchListDict}))

(defn get-watch-list-dict [userID seriesID]
  (get-in (load-user-data userID) [:watchListDict seriesID]) 
  )

(defn save-user-data [userID]
  (map (fn [[_ it]]
         (let [seriesID (get it "seriesID")
               key (str userID "/" seriesID)]
           (db/storeJSON key it)))
       @WATCH_LIST_DICT)
  
  )

(defn update-user-data [userID seriesID attr val] 
  (let [watchlist @WATCH_LIST_DICT
        userObj (get watchlist seriesID)
        userObj (assoc userObj attr val) 
        watchlist (assoc watchlist seriesID userObj)
        ]
    (reset! WATCH_LIST_DICT watchlist)
    )
  )



(defn loadSeasonData [seriesID curSeasonNum] 
   (let [key (str seriesID curSeasonNum)
         seasonData (db/getYAML key)]
     seasonData
     )
  )

(defn get-episode-data [season-data curEpisodeNum]
  (let [curEpisodeNum (Integer/parseInt (str curEpisodeNum))
        maxLen (count season-data)
        _ (assert (and (< curEpisodeNum maxLen) (> curEpisodeNum 0)) "ERROR: Episode Index out of range")
        curEpisodeNum (- curEpisodeNum 1)]
    (nth season-data curEpisodeNum)))


(defn list-all-series [userID]
  (:all-list (load-user-data userID))
  )

(defn list-carry-on-watchlist [userID]
  (:watchList (load-user-data userID)))

(defn get-next-episode [userID seriesID]
  (let [user-obj (get-watch-list-dict userID seriesID)
        curSeasonNum (get user-obj "currentSeasonNumber")
        curEpisodeNum (get user-obj "nextEpisodeNumber")
        season-data (loadSeasonData seriesID curSeasonNum)
        episode-data (get-episode-data season-data curEpisodeNum)
        url (get episode-data "url")]
    (assoc user-obj "url" url)))

(defn play-episode-num [userID seriesID]
  (let [dt-str (now-datetime)]
    (update-user-data userID seriesID "lastWatchedDate" dt-str)
     (save-user-data)
    ))

(defn inc-episode-num 
  ([userID seriesID](inc-episode-num userID seriesID 1))
  ([userID seriesID incNum]
  (let [user-obj (get-watch-list-dict userID seriesID)
        curSeasonNum (get user-obj "currentSeasonNumber")
        curEpisodeNum (get user-obj "nextEpisodeNumber")
        curEpisodeNum (+ curEpisodeNum incNum)
        _ (update-user-data userID seriesID "nextEpisodeNumber" curEpisodeNum)
        eps-obj (get-next-episode userID seriesID)]
    (if eps-obj (save-user-data)
        (let [curSeasonNum (+ curSeasonNum incNum)
              curSeasonNum (if (< curSeasonNum 0) 1 curSeasonNum)
              season-len (count (loadSeasonData seriesID curSeasonNum))
              curEpisodeNum (if (< incNum 0) season-len 1)]
          (update-user-data userID seriesID "currentSeasonNumber" curSeasonNum)
          (update-user-data userID seriesID "nextEpisodeNumber" curEpisodeNum)
          (save-user-data))
        ))))




(comment
  
  (now-datetime)
  
  ;;
  )






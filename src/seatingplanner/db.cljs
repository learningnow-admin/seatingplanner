(ns seatingplanner.db
  (:require
   [cljs.reader :as reader]
   [clojure.edn :as c]
   [re-frame.core :as re-frame]
   ;; [fork.re-frame]
   [cljs.reader]
   [cljs.spec.alpha :as s]
   [seatingplanner.helpers :as h]
   [tick.core :as t]))

(def default-db
  {
   :seatingplanner{
                   :full-screen true
                   :forms {
                           :add-class false
                           :add-student false
                           :add-constraint false
                           :add-layout false
                           :copy-seating-plan false
                           :add-room false
                           :copy-room false
                           :spinner false
                           }

                   :toggle-spot nil
                   :class-id 1
                   :classes (sorted-map
                             1 {:name "Example Class",
                                :students ["Luke" "Amirah" "Chen" "Alejandro" "Jane" "Sally" "Priya" "Matt" "Jamal" "Tyrone" "John" "Fatima"],
                                :constraints [[true :non-adjacent "Alejandro" "John" 6]
                                              [true :non-adjacent "Amirah" "Luke" 2]
                                              [true :proximity "Chen" "Sally" 2]
                                              [false :proximity "Matt" "Jamal" 2]],
                                :seating-plans [{:id 1, :active true}]}
                             )


                   :seating-plans (sorted-map
                                   1 {:name "Example Seating Plan",
                                      :layout [[nil nil nil nil nil nil nil nil nil nil]
                                               [nil :desk nil :desk nil nil nil nil nil nil]
                                               [nil "Jamal" nil "Jane" nil nil "Priya" "Fatima" nil nil]
                                               [nil nil nil nil nil "Matt" :desk :desk "Amirah" nil]
                                               [nil :desk nil :desk nil "Sally" :desk :desk "John" nil]
                                               [nil "Alejandro" nil "Tyrone" nil nil "Chen" "Luke" nil nil]
                                               [nil nil nil nil nil nil nil nil nil nil]
                                               [nil nil nil nil nil nil nil nil nil nil]]}
                                   )


                   :room-id 1
                   :rooms (sorted-map
                           1 {:name "Example Room",
                              :layout [[nil nil nil nil nil nil nil nil nil nil]
                                       [nil :desk nil :desk nil nil nil nil nil nil]
                                       [nil :student nil :student nil nil :student :student nil nil]
                                       [nil nil nil nil nil :student :desk :desk :student nil]
                                       [nil :desk nil :desk nil :student :desk :desk :student nil]
                                       [nil :student nil :student nil nil :student :student nil nil]
                                       [nil nil nil nil nil nil nil nil nil nil]
                                       [nil nil nil nil nil nil nil nil nil nil]]}
                           )
                   }
   })

;; (get-in default-db [:classes 1 :seating-plans 1 :layout])

;; (s/conform even? 1)
;; (s/valid? even? 12)

;;================================
;; SPEC ==========================
;; ===============================
;; (s/def ::seatingplanner.db/db map?)
;; (s/def ::full-screen boolean?)
;; (s/def :: (s/keys :req-un [::id ::title ::done]))
;; (s/def ::full-screen int?)
;; (s/def ::full-screen int?)

;; (s/def ::id int?)
;; (s/def ::title string?)
;; (s/def ::done boolean?)
;; (s/def ::todo (s/keys :req-un [::id ::title ::done]))
;; (s/def ::todos (s/and                                       ;; should use the :kind kw to s/map-of (not supported yet)
;;                 (s/map-of ::id ::todo)                      ;; in this map, each todo is keyed by its :id
;;                 #(instance? PersistentTreeMap %)            ;; is a sorted-map (not just a map)
;;                 ))
;; (s/def ::showing                                            ;; what todos are shown to the user?
;;   #{:all                                                    ;; all todos are shown
;;     :active                                                 ;; only todos whose :done is false
;;     :done                                                   ;; only todos whose :done is true
;;     })
;; (s/def ::db (s/keys :req-un [::todos ::showing]))


;; -- Local Storage  ----------------------------------------------------------
(def ls-key "sp-classes")                         ;; localstore key

(defn seatingplanner->local-store
  "Puts todos into localStorage"
  [classes]
  ;; (js/console.log (str "Hello" classes))
  (.setItem js/localStorage ls-key (str classes)))     ;; sorted-map written as an EDN map

;; -- cofx Registrations  -----------------------------------------------------
;; (re-frame/reg-cofx
;;  :local-store-classes
;;  (fn [cofx _]
;;       ;; put the localstore todos into the coeffect under :local-store-todos
;;    (assoc cofx :local-store-classes
;;              ;; read in todos from localstore, and process into a sorted map
;;           (into (sorted-map)
;;                 (some->> (.getItem js/localStorage ls-key)
;;                          (cljs.reader/read-string)    ;; EDN map -> map
;;                          )))))

(re-frame/reg-cofx
 :local-store-classes
 (fn [cofx _]
   (let [
         ;; custom-tag-map {'time/time (fn [x] (t/time x)),
         ;;                 'time/date-time (fn [x] (t/date-time x))}
         data-from-storage (.getItem js/localStorage ls-key)]


     ;; (reader/register-tag-parser! 'time/time (fn [x] (t/time x)))
     ;; (reader/register-tag-parser! 'time/date-time (fn [x] (t/date-time x)))
     (if-let [parsed-data (when data-from-storage
                            (try
                              (cljs.reader/read-string data-from-storage)
                              ;; (c/read-string {:readers custom-tag-map} data-from-storage)
                              (catch js/Error e
                                (js/console.error "Error parsing data from local storage:" e)
                                nil)))]
       (assoc cofx :local-store-classes {:seatingplanner parsed-data})
       cofx))))


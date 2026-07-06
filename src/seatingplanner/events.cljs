(ns seatingplanner.events
  (:require
   [re-frame.core :as re-frame]
   [seatingplanner.db :as db]
   [seatingplanner.helpers :as h :refer [gdb sdb]]
   [fork.re-frame :as fork]
   [reitit.frontend.easy :as rtfe]
   [clojure.string :as str]
   [cljs.spec.alpha :as s]
   ;; [re-frame.alpha :refer [reg-event-db reg-event-fx inject-cofx path after sub]]
   ;; [todomvc.db    :refer [default-db todos->local-store]]


   [tick.core :as t]))

;;===============================================================================
;; -- Interceptors --------------------------------------------------------------
;; ==============================================================================

(defn check-and-throw
  "Throws an exception if `db` doesn't match the Spec `a-spec`."
  [a-spec db]
  ;; (js/alert (str "a-spec " a-spec " db " db))
  (when-not (s/valid? a-spec db)
    (throw (ex-info (str "spec check failed: " (s/explain-str a-spec db)) {}))))


;; an interceptor using `after`
(def check-spec-interceptor (re-frame/after (partial check-and-throw :seatingplanner.db/default-db
                                                     )))


;;Puts classes into local store
(def ->local-store (re-frame/after db/seatingplanner->local-store))


(def interceptors [
                   (re-frame/path :seatingplanner)
                   ->local-store
                   ])


;; (def interceptors [
;;                    (re-frame/path :class-timers)
;;                    ->local-store])

;;==============================
;; NOTIFICATIONS ================
;;==============================

(re-frame/reg-event-fx
 :show-notification
 (fn [{db :db} [_ message type]]
   (let [id (inc (or (:notification-id db) 0))]
     {:db (-> db
              (assoc :notification {:message message :type type :id id})
              (assoc :notification-id id))
      :dispatch-later {:ms 5000 :dispatch [:clear-notification id]}})))

(re-frame/reg-event-db
 :clear-notification
 (fn [db [_ id]]
   (if (= id (get-in db [:notification :id]))
     (dissoc db :notification)
     db)))

(re-frame/reg-sub
 :notification
 (fn [db _]
   (:notification db)))

;;TODO
;; LOCAL STORE
(re-frame/reg-event-fx
 :initialize-db
 [(re-frame/inject-cofx :local-store-classes)]
 (fn [{:keys [db local-store-classes]} _]
   ;; (js/alert (str "db " db))
   ;; (js/alert (str "local-story-classes " local-store-classes))

   (if (empty? local-store-classes)
     {:db db/default-db}
     {:db (assoc-in local-store-classes [:seatingplanner :forms :spinner] false)}))
 )

;;==============================
;; ADD CLASS ===================
;;==============================
(re-frame/reg-event-fx
 :add-class
 interceptors
 (fn [{db :db} [_ {:keys [values dirty path]}]]
   (js/console.log (str db))
   (let [
         classes (:classes db)
         next-id (h/allocate-next-id classes)
         class-name (get values "input")
         area (get values "area")
         students (h/clean-values area)
         ]
     {:db
      (-> db
          (assoc-in [:forms :add-class] false)
          (assoc :classes
                 (h/create-item classes next-id {:name class-name
                                                 :students (vec students)
                                                 :constraints []
                                                 :seating-plans []
                                                 }
                                )))
      })))


(re-frame/reg-sub
 :add-class-form-status
 (fn [db _ ]
   (get-in db [:seatingplanner :forms :add-class])
   ))

(re-frame/reg-event-db
 :toggle-add-class-form-status
 interceptors
 (fn [db _ ]
   (let [status (get-in db [:forms :add-class])]
    (assoc-in db [:forms :add-class] (not status)))
   ))

(re-frame/reg-event-db
 :delete-class
 interceptors
 (fn [db [_ class-id]]
   (update-in db [:classes] dissoc class-id)
   )
 )


;;==============================
;; GET CLASS ===================
;;==============================
(re-frame/reg-sub
 :classes
 (fn [db _]
   (:classes (:seatingplanner db))))

(re-frame/reg-sub
 :class-id
 (fn [db _]
   (:class-id (:seatingplanner db))))


;;==============================
;; ADD STUDENT =================
;;==============================
(re-frame/reg-event-db
 :delete-student
 interceptors
 (fn [db [_ class-id name]]
   (let [students (get-in db [:classes class-id :students])
         new-students (h/remove-item name students)]
     (assoc-in db [:classes class-id :students] new-students)
     )))

(defn student-exists? [name names]
  (reduce #(or %1 (= %2 name)) false names)
  )



(defn convert-room-to-students [room]
  (let [rows (count room)
        cols (count (first room))]
    (set
     (for [row (range rows)
           col (range cols)
           :when (= (get-in room [row col]):student)]
       [row col]
       ))))

(defn add-student-to-new-row [name seating-plan]
  (let [length (count (first seating-plan))
        ]
        (vec (conj (repeat length nil) name))))

(defn add-new-student [name seating-plan]
  (let [spaces (first (convert-room-to-students seating-plan))]
    ;; (js/alert (str spaces))
    (if spaces
      (assoc-in seating-plan spaces name)
      (conj seating-plan (add-student-to-new-row name seating-plan));;(js/alert "false")
      )))

(re-frame/reg-event-fx
 :add-student
 interceptors
 (fn [{db :db} [_ class-id active-class-seating-plan-id {:keys [values dirty path]}]]
   (let [student-name (get values "input")
         students (get-in db [:classes class-id :students])
         student-exists? (student-exists? student-name students)
         seating-plan (get-in db [:seating-plans, active-class-seating-plan-id, :layout])
         ]
     ;; (js/alert (str "active-class-eating-pland-id " seating-plan ))
     (if student-exists?
       {:dispatch [:show-notification (str student-name " is already in this class.") :error]}
       {:db
        (-> db
            (assoc-in [:seating-plans, active-class-seating-plan-id, :layout] (add-new-student student-name seating-plan))
            (assoc-in [:forms :add-student] false)
            (assoc-in [:classes class-id :students] (conj students student-name) )
            )}))))


(defn student-not-in-seating-plan? [student seating-plan]
  (some #(= student %) (flatten seating-plan))
  )

;;TODO check if student exists on this seating plan
(re-frame/reg-event-fx
 :student-to-seating-plan
 interceptors
 (fn [{db :db} [_ student-name active-class-seating-plan-id]]
   (let [seating-plan (get-in db [:seating-plans, active-class-seating-plan-id, :layout])]
     (if (student-not-in-seating-plan? student-name seating-plan)
       {:dispatch [:show-notification (str student-name " is already in the seating plan.") :warning]}
       {:db (assoc-in db [:seating-plans, active-class-seating-plan-id, :layout] (add-new-student student-name seating-plan))}
     ))))


;; #(re-frame/dispatch [:student-to-seating-plan student active-class-seating-plan-id])



(re-frame/reg-sub
 :add-student-form-status
 (fn [db _ ]
   (get-in db [:seatingplanner :forms :add-student])
   ))

(re-frame/reg-event-db
 :toggle-add-student-form-status
 interceptors
 (fn [db _ ]
   (let [status (get-in db [:forms :add-student])]
    (assoc-in db [:forms :add-student] (not status)))
   ))





;;==============================
;; CONSTRAINTS =================
;;==============================

(re-frame/reg-event-db
 :delete-constraint
 interceptors
 (fn [db [_ class-id constraint]]
   (let [constraints (get-in db [:classes class-id :constraints])
         new-constraints (h/remove-item constraint constraints)
         ]
     (assoc-in db [:classes class-id :constraints] new-constraints)
     )))

(re-frame/reg-event-db
 :toggle-constraint
 interceptors
 (fn [db [_ class-id [c t s1 s2 d :as constraint]]]
   (let [
         constraints (get-in db [:classes class-id :constraints])
         new-constraint [(not c) t s1 s2 d]
         new-constraints (map (fn [x] (if (= constraint x) new-constraint x)) constraints)
         ]
     (assoc-in db [:classes class-id :constraints] new-constraints))))


(re-frame/reg-event-fx
 :add-constraint
 interceptors
 (fn [{db :db} [_ class-id {:keys [values dirty path]}]]
   (let [s1 (get values "s1")
         s2 (get values "s2")
         type (get values "type")
         space (get values "space")
         new-constraint [true (keyword type) s1 s2 (int space)]
         constraints (get-in db [:classes class-id :constraints])]
     {:db
      (-> db
          (assoc-in [:forms :add-constraint] false)
          (assoc-in [:classes class-id :constraints] (vec (conj constraints new-constraint)) )
          )}
     )))

 (re-frame/reg-sub
  :add-constraint-form-status
  (fn [db _ ]
    (get-in db [:seatingplanner :forms :add-constraint])
    ))

(re-frame/reg-event-db
 :toggle-add-constraint-form-status
 interceptors
 (fn [db _ ]
   (let [status (get-in db [:forms :add-constraint])]
    (assoc-in db [:forms :add-constraint] (not status)))
   ))

;;==============================
;; LAYOUTS =====================
;;==============================
(def foo '({:id 4, :active false} {:id 2, :active false}))
(assoc-in (vec foo) [0 :active] true)


(re-frame/reg-event-db
 :delete-layout
 interceptors
 (fn [db [_ class-id id]]
   (let [class-seating-plans (get-in db [:classes, class-id, :seating-plans])
         removed (filter #(not= id (:id %)) class-seating-plans)
         updated-class-seating-plans (assoc-in (vec removed) [0 :active] true)
         ]
     ;; (js/alert (str updated-class-seating-plans))
     (-> db
         (update-in [:seating-plans] dissoc id)
         (assoc-in [:classes, class-id :seating-plans] updated-class-seating-plans)
         ))))


(defn add-id-change-active-status [data new-id]
  (let [updated-data (map #(assoc % :active false) data)]
    (conj updated-data {:id new-id, :active true})))

(re-frame/reg-event-fx
 :add-layout
 interceptors
 (fn [{db :db} [_ class-id {:keys [values dirty path]}]]
   (let [
         ;; ADDING TO SEATING PLAN

         room-id (int (get values "room"))
         seating-plans (:seating-plans db)


         new-seating-plan-id (h/allocate-next-id seating-plans)
         new-seating-plan-layout (if (= 0 room-id) [[nil nil] [nil nil]] (get-in db [:rooms, room-id :layout]))
         new-seating-plan-name (get values "name")
         new-seating-plan {:name new-seating-plan-name, :layout new-seating-plan-layout}
         ;; ;; ADDING TO CLASS SEATING PLAN
         class-seating-plans (get-in db [:classes, class-id, :seating-plans])
         new-class-seating-plans (add-id-change-active-status class-seating-plans new-seating-plan-id)
         ;; new-layout [(keyword type) s1 s2 (int space)]
         ;; width (get values "w")
         ;; height (get values "h")
         ;; layouts (get-in db [:classes class-id :layouts])
         ]
     ;; (js/alert (type new-seating-plan-name))
     {:db

      (-> db
          (assoc-in [:forms :add-layout] false)
          (assoc-in [:classes, class-id, :seating-plans] new-class-seating-plans)
          (assoc :seating-plans (h/update-item seating-plans new-seating-plan-id new-seating-plan))
          )}
     )))


(re-frame/reg-event-db
 :change-layout
 interceptors
 (fn [db [_ class-id active-layout-id]]
   (let [seating-plans (get-in db [:classes, class-id, :seating-plans])
         new-seating-plans (map #(if (= (:id %) (int active-layout-id))
                                   (assoc % :active true)
                                   (assoc % :active false)) seating-plans)]
     (assoc-in db [:classes, class-id, :seating-plans] new-seating-plans)
     )))

(re-frame/reg-sub
 :add-layout-form-status
 (fn [db _ ]
   (get-in db [:seatingplanner :forms :add-layout])
   ))

(re-frame/reg-event-db
 :toggle-add-layout-form-status
 interceptors
 (fn [db _ ]
   (let [status (get-in db [:forms :add-layout])]
    (assoc-in db [:forms :add-layout] (not status)))
   ))



(re-frame/reg-event-db
 :toggle-copy-seating-plan-form-status
 interceptors
 (fn [db _]
   (let [status (get-in db [:forms :copy-seating-plan])]
    (assoc-in db [:forms :copy-seating-plan] (not status)))
   ))

(re-frame/reg-sub
 :copy-seating-plan-form-status
 (fn [db _]
   (get-in db [:seatingplanner :forms :copy-seating-plan])))


(re-frame/reg-event-fx
 :copy-seating-plan
 interceptors
 (fn [{db :db} [_ {:keys [values dirty path]} class-id active-class-seating-plan-id]]
   (let [
         ;; ADDING TO SEATING PLAN

         ;; room-id (int (get values "room"))
         seating-plans (:seating-plans db)

         new-seating-plan-id (h/allocate-next-id seating-plans)


         new-seating-plan-layout (get-in seating-plans [active-class-seating-plan-id, :layout])

         new-seating-plan-name (get values "name")
         new-seating-plan {:name new-seating-plan-name, :layout new-seating-plan-layout}
         ;; ;; ADDING TO CLASS SEATING PLAN
         class-seating-plans (get-in db [:classes, class-id, :seating-plans])
         new-class-seating-plans (add-id-change-active-status class-seating-plans new-seating-plan-id)
         ]
     {:db

      (-> db
          (assoc-in [:forms :copy-seating-plan] false)
          (assoc-in [:classes, class-id, :seating-plans] new-class-seating-plans)
          (assoc :seating-plans (h/update-item seating-plans new-seating-plan-id new-seating-plan))
          )}
     )))


(re-frame/reg-event-fx
 :validate
 interceptors
 (fn [{db :db} [_ class-id active-class-seating-plan-id]]
   (let [
         students (set (get-in db [:classes, class-id, :students]))
         constraints (get-in db [:classes, class-id, :constraints])
         seating-plan (get-in db [:seating-plans, active-class-seating-plan-id, :layout])
         students-in-seating-plan (set (filter #(string? %) (flatten seating-plan)))
         missing (clojure.set/difference students students-in-seating-plan)
         extras (clojure.set/difference students-in-seating-plan students)
         constraints-met? (h/check-constraints? seating-plan constraints)
         ]
     {:db (assoc db :validation-result {:missing missing
                                        :extras extras
                                        :constraints-met? constraints-met?})})))

(re-frame/reg-sub
 :validation-result
 (fn [db _]
   (:validation-result (:seatingplanner db))))



;;==============================
;; FULL SCREEN =================
;;==============================
(re-frame/reg-event-db
 :full-screen-toggle
 interceptors
 (fn [db _ ]
   (assoc db :full-screen (not (:full-screen db))))
 )

(re-frame/reg-sub
 :full-screen
 (fn [db _ ]
    (:full-screen (:seatingplanner db)))
   )



;;==============================
;; SEATING PLANS ===============
;;==============================
(re-frame/reg-sub
 :seating-plans
 (fn [db _ ]
   (get-in db [:seatingplanner :seating-plans])
   ))


;;==============================
;; ROOMS =======================
;;==============================
(re-frame/reg-event-db
 :room-id
 interceptors
 (fn [db [_ id]]
   (assoc db :room-id id)
   ))

(re-frame/reg-sub
 :room-id
 (fn [db _]
   (:room-id (:seatingplanner db))))

(re-frame/reg-sub
 :rooms
 (fn [db _]
   (:rooms (:seatingplanner db))))

(re-frame/reg-sub
 :room
 (fn [db [_ id]]
   (get-in db [:seatingplanner :rooms id])))





(re-frame/reg-event-fx
 :add-room
 interceptors
 (fn [{db :db} [_ {:keys [values dirty path]}]]
   (let [
         rooms (:rooms db)
         next-id (h/allocate-next-id rooms)
         room-name (get values "input")
         width (get values "w")
         height (get values "h")]
     {:db
      (-> db
          (assoc-in [:forms :add-room] false)
          (assoc :rooms
                 (h/create-item rooms next-id {:name room-name
                                               :layout (vec (repeat height (vec (repeat width nil))))
                                               })))})))

(re-frame/reg-event-db
 :delete-room
 interceptors
 (fn [db [_ room-id]]
   (update-in db [:rooms] dissoc room-id)
   )
 )

(re-frame/reg-sub
 :add-room-form-status
 (fn [db _]
   (get-in db [:seatingplanner :forms :add-room])
   ))

(re-frame/reg-event-db
 :toggle-add-room-form-status
 interceptors
 (fn [db _ ]
   (let [status (get-in db [:forms :add-room])]
     (assoc-in db [:forms :add-room] (not status)))
   ))

;;==============================
;; COPY ROOM ===================
;;==============================

(re-frame/reg-event-fx
 :copy-room
 interceptors
 (fn [{db :db} [_ {:keys [values dirty path]} room-id]]
   (let [
         rooms (:rooms db)
         room-layout (get-in rooms [room-id, :layout])
         next-id (h/allocate-next-id rooms)
         room-name (get values "input")

         ;; width (get values "w")
         ;; height (get values "h")
         ]
;; (js/alert (str room-layout))

     {:db
      (-> db
          (assoc-in [:forms :copy-room] false)
          (assoc :rooms
                 (h/create-item rooms next-id {:name room-name
                                               :layout room-layout ;; (vec (repeat 12 (vec (repeat 12 nil))))
                                               })))})))


(re-frame/reg-event-db
 :toggle-copy-room-form-status
 interceptors
 (fn [db _ ]
   (let [status (get-in db [:forms :copy-room])]
     (assoc-in db [:forms :copy-room] (not status)))
   ))

(re-frame/reg-event-db
 :toggle-on-copy-room-form-status
 interceptors
 (fn [db [_ room-id] ]
   (let [status (get-in db [:forms :copy-room])]
     (-> db
      (assoc-in [:forms :copy-room] (not status))
      (assoc :room-id room-id)
      ))
   ))


(re-frame/reg-sub
 :copy-room-form-status
 (fn [db _]
   (get-in db [:seatingplanner :forms :copy-room])
   ))








;;==============================
;; EDITOR ======================
;;==============================

;;TODO write some constructors

(re-frame/reg-event-db
 :clear-all
interceptors
 (fn [db [_ path]]
   (let [layout (get-in db path)
         num-rows (count layout)
         num-columns (count (first layout))
         new-layout (vec (repeat num-rows (vec (repeat num-columns nil))))]
     (assoc-in db path new-layout)
     )))



(re-frame/reg-event-db
 :change-cell
 interceptors
 (fn [db [_ path row column]]
   (let [toggle-spot (:toggle-spot db)
         layout (get-in db path)
         new-layout (assoc-in layout [row column] toggle-spot)]
     ;; (js/alert (str "from change-cell\n layout " layout "\nnewlayout " new-layout))
     (assoc-in db path new-layout))))


;; (def foo
;; [["Sallly" :student nil] [:student :student nil] [nil nil "Jack"] [:desk "Jill" :student]]
;;   )

;; (def dragging-id-d [0 0])
;; (def valid-drop-id-d [2 2])

;; ;;1. get the value for both
;; ;;2. put the value into each

;; (get-in foo dragging-id-d)

;; (assoc-in foo dragging-id-d "Hello")

;; (-> foo
;;     (assoc-in dragging-id-d "Hello")
;;     (assoc-in valid-drop-id-d "MOO")
;;     )


(re-frame/reg-event-db
 :swap-cells
 interceptors
 (fn [db [_ path dragging-id valid-drop-id]]
   (let [
         layout (get-in db path)
         c1 (get-in layout dragging-id)
         c2 (get-in layout valid-drop-id)
         new-layout (-> layout
                         (assoc-in dragging-id c2)
                         (assoc-in valid-drop-id c1)
                     )
         ;; toggle-spot (:toggle-spot db)
         ;;       layout (get-in db path)
         ;;       new-layout (assoc-in layout [row column] toggle-spot)
         ]
       ;; (assoc-in db path new-layout)
     ;; (js/alert (str "path " path " ""dragging-id " dragging-id " valid-drop-id " valid-drop-id))
     ;; (js/alert (str "layout " (str layout) "\nnewlayout " (str new-layout) ))
     ;; (js/alert (str "c1 " c1 " c2 " c2))
     ;; db
     (assoc-in db path new-layout)
     )))


(re-frame/reg-event-db
 :toggle-spot
 interceptors
 (fn [db [_ type]]
   (assoc db :toggle-spot type)))

(re-frame/reg-sub
 :toggle-spot
 (fn [db _]
   (:toggle-spot (:seatingplanner db))))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; (def map   {:seating-plans (sorted-map
;;                             1 {:name "C4 Computer Lap"
;;                                :layout [[nil :student nil]
;;                                         [:student :student nil]
;;                                         [nil nil "Jack"]
;;                                         [:desk nil :student]]
;;                                }

;;                             2 {:name "B2 Science Lap"
;;                                :layout [["Sallly" :student nil]
;;                                         [:student :student nil]
;;                                         [nil nil "Jack"]
;;                                         [:desk "Jill" :student]]
;;                                }
;;                             )
;;             })

;; (update-in map [:seating-plans] dissoc 1)

;; (def foo '({:id 2, :active false} {:id 1, :active true}))

;; (def foo '({:id 2, :active false} {:id 1, :active true}))
;; (filter #(not= 1 (:id %)) foo)

;; layout (get-in db path)
;; new-layout (assoc-in layout [row column] toggle-spot)]
;; (assoc-in db path new-layout))))

(re-frame/reg-event-db
 :add-column-right
 interceptors
 (fn [db [_ path]]
   (let [
         layout (get-in db path)
         new-layout (into [] (map #(conj % nil) layout))]
     (assoc-in db path new-layout ))))

(re-frame/reg-event-db
 :remove-column-right
 interceptors
 (fn [db [_ path]]
   (let [
         layout (get-in db path)
         new-layout (into [] (map #(pop %) layout))]
     (assoc-in db path new-layout ))))

(re-frame/reg-event-db
 :add-column-left
 interceptors
 (fn [db [_ path]]
   (let [layout (get-in db path)
         new-layout (vec (map #(conj (vec (cons nil %))) layout))]
     (assoc-in db path new-layout))))

(re-frame/reg-event-db
 :remove-column-left
 interceptors
 (fn [db [_ path]]
   (let [layout (get-in db path)
         new-layout (vec (map (fn [[n & rest]] (vec rest)) layout))]
     (assoc-in db path new-layout))))

;; (def doo
;; [[nil :student nil]
;; [nil "Foo" :desk]]
;;   )
;; (vec (map #(conj (vec (cons nil %))) doo))

;; new-layout (vec (map (fn [[n & rest]]
;;                        (println n)
;;                        (vec rest)) doo))
;; (vec (map #(conj % nil) doo))

(re-frame/reg-event-db
 :add-row-top
 interceptors
 (fn [db [_ path]]
   (let [layout (get-in db path)
         new-row (-> layout
                     first
                     count
                     (repeat nil)
                     vec)
         new-layout (conj (vec (cons new-row layout)))]
     (assoc-in db path new-layout ))))

(re-frame/reg-event-db
 :remove-row-top
 interceptors
 (fn [db [_ path]]
   (let [layout (get-in db path)
         new-layout (vec (rest layout))]
     (assoc-in db path new-layout ))))

(re-frame/reg-event-db
 :add-row-bottom
 interceptors
 (fn [db [_ path]]

   (let [
         layout (get-in db path)
         new-row (-> layout
                     first
                     count
                     (repeat nil)
                     vec)
         new-layout (conj layout new-row)]
     (assoc-in db path new-layout ))))

(re-frame/reg-event-db
 :remove-row-bottom
 interceptors
 (fn [db [_ path]]
   (let [
         layout (get-in db path)
         new-layout (pop layout)]
     (assoc-in db path new-layout ))))

;;==============================
;; MODIFY ROOM =================
;;==============================
;; (re-frame/reg-sub
;;  :classroom
;;  (fn [db _]
;;    (:classroom db)
;;    )
;;  )

;;TODO NEEDS TO BE UPDATED
;; (re-frame/reg-sub
;;  :class-temp
;;  :<- [:classes]
;;  (fn [classes _]
;;    (first classes)
;;    ))

(re-frame/reg-sub
 :get-class
 (fn [db [_ id]]
   ;; (js/alert
    ;; (get classes id)
   (h/read-item (get-in db [:seatingplanner :classes]) id)
   )
 )

(re-frame/reg-event-db
 :class-id
 interceptors
 (fn [db [_ id]]
   (assoc-in db [:class-id] id)
   )
 )

;;SELECT CLASS ===

(re-frame/reg-event-fx
 :auto-fill
 interceptors
 (fn [{db :db} [_ class-id seating-plan-id]]
   (let [
         class (get-in db [:classes, class-id])
         students (:students class)
         constraints (:constraints class)
         layout (get-in db [:seating-plans, seating-plan-id, :layout])
         result (h/generate-seating-plan layout students constraints)]
     (cond-> {:db (-> db
                      (assoc-in [:forms :spinner] false)
                      (assoc-in [:seating-plans seating-plan-id, :layout] (:layout result)))}
       (:error result)
       (assoc :dispatch [:show-notification (:error result) :error])))))

(re-frame/reg-event-fx
 :organise
 interceptors
 (fn [{:keys [db]} [_ class-id seating-plan-id]]
   {:db (assoc-in db [:forms :spinner] true)
    ;; :dispatch [:auto-fill class-id seating-plan-id]
    :dispatch-later {:ms 200
                     :dispatch [:auto-fill class-id seating-plan-id]}

    }))

(re-frame/reg-sub
 :spinner
 (fn [db _]
   (:spinner (:forms (:seatingplanner db)))))

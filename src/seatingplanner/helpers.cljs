(ns seatingplanner.helpers
(:require
   [tick.core :as t]
   [clojure.string :as str]
   [tick.alpha.interval :as t.i]
   [goog.string :as gstring]
   ))

(defn sdb [path]
  (fn [db [_ v]]
    (assoc-in db path v)))

(defn gdb
  [path]
  (fn [db _] (get-in db path)))

;;===============================================
;; STRING CONVERSIONS
;;===============================================

(defn clean-values [value]
  (->> (str/split value #"[,\n]")
       (map str/trim)
       (vec)
       (distinct)
       ))




(defn remove-item [item items]
  (vec (remove #(= item %) items)))
;;==============================================
;; ID MANAGEMENT HELPER FUNCTIONS ==============
;;==============================================
(defn create-item [items id data]
  (assoc items id data))

(defn read-item [items id]
  (get items id))

(defn update-item [items id new-data]
  (assoc items id new-data))

(defn delete-item [items id]
  (dissoc items id))

(defn allocate-next-id
  "Returns the next id for the next item.
  Assumes items are sorted.
  Returns one more than the current largest id."
  [items]
  ((fnil inc 0) (last (keys items))))

;; (def items (sorted-map))
;; (def items (create-item items 1 "Data for item 1"))
;; (def items (create-item items 2 "Data for item 2"))
;; (def items (update-item items 2 "Updated data for item 2"))

;; (sorted-map
;; 2 "Hellosdf"
;;  )


;; (def foolasses (sorted-map
;;                 1 {
;;                    :name "Year 7 Digital Technology"
;;                    :students ["Sally" "Noah" "John" "James"]
;;                    :constraints ["??"]
;;                    :seating-plans (sorted-map
;;                                    1
;;                                    {:name "Hellow"
;;                                     :layout [[:person :nil "Sally"]
;;                                              [:nil :desk "Noah"]
;;                                              ["John" "James" :nil]]}
;;                                    )}




;;                 2 {
;;                    :name "Year 10 Digital Technology"
;;                    :students ["Mally" "Jill" "Eleanor" "Alan"]
;;                    :constraints ["??"]
;;                    :seating-plans (sorted-map
;;                                    1
;;                                    {:name "Hellow"
;;                                     :layout [[:person :nil "Sally"]
;;                                              [:nil :desk "Noah"]
;;                                              ["John" "James" :nil]]}
;;                                    )}


;;                 )
;;   )

;; (for [[class-id class] foolasses
;;       ;; [seating-id seating-plan] (:seating-plans class)

;;       ]
;;   ;; (println "Class ID:" class-id)
;;   (println "Name:" (str class))
;;   ;; (println "Students:" (:students class))
;;   ;; (println "Constraints:" (:constraints class))
;;   ;; (println "Seating Plan ID:" seating-id)
;;   ;; (println "Seating Plan Name:" (:name seating-plan))
;;   ;; (println "Seating Plan Layout:" (:layout seating-plan)

;;   ;;          )

;;   )


;;==============================================
;; CONSTRAINT SATISFACTION PROBLEM =============
;;==============================================
;;
;;
;;
;;



(defn distance [seat1 seat2]
  (let [[x1 y1] seat1
        [x2 y2] seat2
        dx (- x2 x1)
        dy (- y2 y1)]
    (Math/sqrt (+ (* dx dx) (* dy dy)))))

(distance [0 0] [1 2])
(distance [1 2] [0 0])

(distance [0 0] [3 0])
(distance [3 0] [0 0])

(distance [3 0] [1 2])
(distance [1 2] [3 0])

(def counter (atom 0))

(defn order-students
  "Sorts students so those involved in active constraints come first.
   This is an MRV-like heuristic — placing the most constrained students
   first dramatically reduces backtracking.  Within each group, the order
   is randomised so repeated Autofill runs produce different plans."
  [students constraints]
  (let [enabled  (filter first constraints)
        counts   (frequencies (mapcat (fn [[_ _ s1 s2 _]] [s1 s2]) enabled))
        with-c   (shuffle (filter #(pos? (get counts % 0)) students))
        without-c (shuffle (remove #(pos? (get counts % 0)) students))]
    (vec (concat with-c without-c))))

(defn backtrack*
  "Depth-first backtracking CSP solver.
   - assignment : map of student→seat built so far
   - remaining  : students still to place (ordered)
   - domain     : set of unoccupied seat positions
   - violated?  : (fn [assignment]) → true if any constraint is broken
   Returns a complete assignment map, or nil if no solution exists."
  [assignment remaining domain violated?]
  (cond
    (empty? remaining) assignment
    :else
    (let [student   (first remaining)
          rest-vars (rest remaining)]
      ;; Shuffle the available seats so each run explores a different order.
      (loop [seats (shuffle (seq domain))]
        (when (seq seats)
          (let [seat           (first seats)
                new-assignment (assoc assignment student seat)]
            (swap! counter inc)
            (if (violated? {:assignment new-assignment})
              ;; Constraint broken — try the next seat
              (recur (rest seats))
              ;; Valid so far — recurse deeper
              (or (backtrack* new-assignment rest-vars (disj domain seat) violated?)
                  ;; That branch failed — try the next seat
                  (recur (rest seats))))))))))

;; (next-csps (first (next-csps example-csp)))

;; => ({:domains #{[0 0] [3 0] [2 0] [1 2]},
;;      :variables ["Jill" "Sally" "John" "James"],
;;      :any-constraints-violated? #object [cljs$core$sp2],
;;      :assignment {"Jill" [2 2], "Sally" nil, "John" nil, "James" nil}}

;; => ({:domains #{[3 0] [2 0] [1 2]},
;;      :variables ["Jill" "Sally" "John" "James"],
;;      :any-constraints-violated? #object [cljs$core$sp2],
;;      :assignment {"Jill" [2 2], "Sally" [0 0], "John" nil, "James" nil}}
;;     {:domains #{[0 0] [2 0] [1 2]},
;;      :variables ["Jill" "Sally" "John" "James"],
;;      :any-constraints-violated? #object [cljs$core$sp2],
;;      :assignment {"Jill" [2 2], "Sally" [3 0], "John" nil, "James" nil}}
;;     {:domains #{[0 0] [3 0] [1 2]},
;;      :variables ["Jill" "Sally" "John" "James"],
;;      :any-constraints-violated? #object [cljs$core$sp2],
;;      :assignment {"Jill" [2 2], "Sally" [2 0], "John" nil, "James" nil}}
;;     {:domains #{[0 0] [3 0] [2 0]},
;;      :variables ["Jill" "Sally" "John" "James"],
;;      :any-constraints-violated? #object [cljs$core$sp2],
;;      :assignment {"Jill" [2 2], "Sally" [1 2], "John" nil, "James" nil}})







;; backtracking-seq removed — replaced by backtrack* above



;; (let [[$first & $rest](seq (next-csps example-csp))]
;;   (consistent? $first)
;;   )
;; => {:domains #{[0 0] [3 0] [2 0] [1 2]},
;;     :variables ["Jill" "Sally" "John" "James"],
;;     :any-constraints-violated? #object [cljs$core$sp2],
;;     :assignment {"Jill" [2 2], "Sally" nil, "John" nil, "James" nil}}



;; backtracking removed — replaced by backtrack* above


;; (def example-csp
;;  {:domains #{[0 0] [1 2] [2 0] [2 2] [3 0]},
;;   :variables ["Jill" "Sally" "John" "James"],
;;   :any-constraints-violated?(apply some-fn #{(constraint :non-adjacent "James" "John" 2)
;;                                              (constraint :proximity "Jill" "Sally" 1)
;;                                              }),
;;   :assignment {"Jill" nil, "Sally" nil, "John" nil, "James" nil}}
;;   )



;; (take 10 (filter complete? (backtracking-seq (next-csps example-csp))))
;; => ({:domains #{[2 0]},
;;      :variables ["Jill" "Sally" "John" "James"],
;;      :any-constraints-violated? #object [cljs$core$sp2],
;;      :assignment {"Jill" [2 2], "Sally" [1 2], "John" [0 0], "James" [3 0]}}
;;     {:domains #{},
;;      :variables ["Jill" "Sally" "John" "James"],
;;      :any-constraints-violated? #object [cljs$core$sp2],
;;      :assignment
;;      {"Jill" [2 2], "Sally" [1 2], "John" [0 0], "James" [3 0], nil [2 0]}}
;;     {:domains #{[2 0]},
;;      :variables ["Jill" "Sally" "John" "James"],
;;      :any-constraints-violated? #object [cljs$core$sp2],
;;      :assignment {"Jill" [2 2], "Sally" [1 2], "John" [3 0], "James" [0 0]}}
;;     {:domains #{},
;;      :variables ["Jill" "Sally" "John" "James"],
;;      :any-constraints-violated? #object [cljs$core$sp2],
;;      :assignment
;;      {"Jill" [2 2], "Sally" [1 2], "John" [3 0], "James" [0 0], nil [2 0]}}
;;     {:domains #{[1 2]},
;;      :variables ["Jill" "Sally" "John" "James"],
;;      :any-constraints-violated? #object [cljs$core$sp2],
;;      :assignment {"Jill" [3 0], "Sally" [2 0], "John" [2 2], "James" [0 0]}}
;;     {:domains #{},
;;      :variables ["Jill" "Sally" "John" "James"],
;;      :any-constraints-violated? #object [cljs$core$sp2],
;;      :assignment
;;      {"Jill" [3 0], "Sally" [2 0], "John" [2 2], "James" [0 0], nil [1 2]}}
;;     {:domains #{[1 2]},
;;      :variables ["Jill" "Sally" "John" "James"],
;;      :any-constraints-violated? #object [cljs$core$sp2],
;;      :assignment {"Jill" [3 0], "Sally" [2 0], "John" [0 0], "James" [2 2]}}
;;     {:domains #{},
;;      :variables ["Jill" "Sally" "John" "James"],
;;      :any-constraints-violated? #object [cljs$core$sp2],
;;      :assignment
;;      {"Jill" [3 0], "Sally" [2 0], "John" [0 0], "James" [2 2], nil [1 2]}}
;;     {:domains #{[2 2]},
;;      :variables ["Jill" "Sally" "John" "James"],
;;      :any-constraints-violated? #object [cljs$core$sp2],
;;      :assignment {"Jill" [3 0], "Sally" [2 0], "John" [0 0], "James" [1 2]}}
;;     {:domains #{},
;;      :variables ["Jill" "Sally" "John" "James"],
;;      :any-constraints-violated? #object [cljs$core$sp2],
;;      :assignment
;;      {"Jill" [3 0], "Sally" [2 0], "John" [0 0], "James" [1 2], nil [2 2]}})

;; (next-csps example-csp)

;; (next-csps example-csp)
;; => ({:domains #{[0 0] [3 0] [2 0] [1 2]},
;;      :variables ["Jill" "Sally" "John" "James"],
;;      :any-constraints-violated? #object [cljs$core$sp2],
;;      :assignment {"Jill" [2 2], "Sally" nil, "John" nil, "James" nil}}
;;     {:domains #{[2 2] [3 0] [2 0] [1 2]},
;;      :variables ["Jill" "Sally" "John" "James"],
;;      :any-constraints-violated? #object [cljs$core$sp2],
;;      :assignment {"Jill" [0 0], "Sally" nil, "John" nil, "James" nil}}
;;     {:domains #{[2 2] [0 0] [2 0] [1 2]},
;;      :variables ["Jill" "Sally" "John" "James"],
;;      :any-constraints-violated? #object [cljs$core$sp2],
;;      :assignment {"Jill" [3 0], "Sally" nil, "John" nil, "James" nil}}
;;     {:domains #{[2 2] [0 0] [3 0] [1 2]},
;;      :variables ["Jill" "Sally" "John" "James"],
;;      :any-constraints-violated? #object [cljs$core$sp2],
;;      :assignment {"Jill" [2 0], "Sally" nil, "John" nil, "James" nil}}
;;     {:domains #{[2 2] [0 0] [3 0] [2 0]},
;;      :variables ["Jill" "Sally" "John" "James"],
;;      :any-constraints-violated? #object [cljs$core$sp2],
;;      :assignment {"Jill" [1 2], "Sally" nil, "John" nil, "James" nil}})



(defmulti constraint (fn [type & args] type))

(defmethod constraint :non-adjacent [type & args]
  (let [[student1 student2 d] args]
    (fn [csp]
      (if-let [s1 (get-in csp [:assignment student1])]
        (if-let [s2 (get-in csp [:assignment student2])]
          (<= (distance s1 s2) d) ;;If this is true, then it is bad!
          false)
        false
        ))))

;; (defmethod constraint :proximity [type & args]
;;   (let [[student1 student2 d] args]
;;     (fn [csp]
;;       (if-let [s1 (get-in csp [:assignment student1])]
;;         (> (distance s1 (get-in csp [:assignment student2])) d) ;;If this is true, then it is bad!
;;         false ;;false means there is no error
;;         ))))



(defmethod constraint :proximity [type & args]
  (let [[student1 student2 d] args]
    (fn [csp]
      (if-let [s1 (get-in csp [:assignment student1])]
        (if-let [s2 (get-in csp [:assignment student2])]
          (> (distance s1 s2) d) ;; If this is true, then it is bad!
          false)
        false)))) ;; false means there is no error


(defmethod constraint :empty [type & args]
  (fn [csp]
    false))

(defn set-up-constraints [constraints]
  (let [constraints-filtered (filter (fn [x] (first x)) constraints)]
  (if (empty? constraints-filtered)
    (set [(constraint :empty, "", "", 0)])
    (set (map (fn [[c t s1 s2 d]] (constraint t s1 s2 d)) constraints-filtered))))
)
;;==============================================
;; ROOM CONVERSIONS ============================
;;==============================================
(defn convert-room-to-seats [room]
  (let [rows (count room)
        cols (count (first room))]
    (set
     (for [row (range rows)
           col (range cols)
           :when (and (not= (get-in room [row col]) nil)
                      (not= (get-in room [row col]) :desk))]
       [row col]
       ))))


(defn update-room [room row col new-value]
  (assoc-in room [row col] new-value))

(defn update-room-with-allocation [room allocations]
  (loop [a allocations
         r room]
    (if (empty? a)
      r
      (let [allocation (first a)
            row (first (second allocation))
            coll (second (second allocation))
            new-value (first allocation)]
        (recur (rest a) (update-room r row coll new-value)
               )))))

(defn allocation? [room]
  (boolean (some string? (flatten room))))



(defn vector-dimensions [vector]
  (str
   (apply max (map count vector))
   "x"
   (count vector)
   ))



;; TODO maximum distance. teacher proximal. seating preference
;;==============================================
;; GENERATE SEATING PLAN =======================
;;==============================================

(defn generate-seating-plan [room students constraints]
  (reset! counter 0)
  (let [cleared-room  (mapv (fn [row] (mapv #(if (string? %) :student %) row)) room)
        seats         (convert-room-to-seats cleared-room)
        ordered       (order-students students constraints)
        violated?     (apply some-fn (set-up-constraints constraints))
        result        (when (>= (count seats) (count students))
                        (backtrack* {} ordered seats violated?))
        allocated-room (if result
                         (update-room-with-allocation cleared-room result)
                         cleared-room)]
    (if (allocation? allocated-room)
      {:layout allocated-room :error nil}
      {:layout room
       :error "Could not find an allocation. Try: adding more seats, removing some constraints, or reducing the number of students."})))

;; (defn init [domains variables constraints]
;;   {:domains domains
;;    :variables variables
;;    :any-constraints-violated? (apply some-fn constraints)
;;    :assignment (zipmap variables (repeat nil))})

;; (defn consistent? [{:keys [any-constraints-violated?]
;;                     :as csp}]
;;   (swap! counter inc)
;;   (not (any-constraints-violated? csp)))
;; work out assignments from seating-plan


(defn convert-room-to-assignment [seating-plan]
  (let [rows (count seating-plan)
        cols (count (first seating-plan))]
    (into {}
     (for [row (range rows)
           col (range cols)
           :when (and (not= (get-in seating-plan [row col]) nil)
                      (not= (get-in seating-plan [row col]) :desk)
                      (not= (get-in seating-plan [row col]) :student)
                      )]
       {(get-in seating-plan [row col])
        [row col]}
       ))))

(defn check-constraints? [seating-plan constraints]
  (let [
        csp {
             :any-constraints-violated? (apply some-fn (set-up-constraints constraints))
             :assignment (convert-room-to-assignment seating-plan)
             }
       ]
    (not ((:any-constraints-violated? csp) csp))))

;; (def room [
;;            [:student nil nil]
;;            [nil      nil :student]
;;            [:student nil :student]
;;            [:student nil nil]
;;            ])

;; (def students ["Jill" "Sally" "John" "James"])
;; (generate-seating-plan room students constraints)

;; (def combinations
;;   '(
;;   ("John" "James" "Sally" "Jill")
;;   ("John" "James" "Jill" "Sally")
;;   ("John" "Sally" "James" "Jill")
;;   ("John" "Sally" "Jill" "James")
;;   ("John" "Jill" "James" "Sally")
;;   ("John" "Jill" "Sally" "James")
;;   ("James" "John" "Sally" "Jill")
;;   ("James" "John" "Jill" "Sally")
;;   ("James" "Sally" "John" "Jill")
;;   ("James" "Sally" "Jill" "John")
;;   ("James" "Jill" "John" "Sally")
;;   ("James" "Jill" "Sally" "John")
;;   ("Sally" "John" "James" "Jill")
;;   ("Sally" "John" "Jill" "James")
;;   ("Sally" "James" "John" "Jill")
;;   ("Sally" "James" "Jill" "John")
;;   ("Sally" "Jill" "John" "James")
;;   ("Sally" "Jill" "James" "John")
;;   ("Jill" "John" "James" "Sally")
;;   ("Jill" "John" "Sally" "James")
;;   ("Jill" "James" "John" "Sally")
;;   ("Jill" "James" "Sally" "John")
;;   ("Jill" "Sally" "John" "James")
;;   ("Jill" "Sally" "James" "John")))



;; (def constraints [[true :non-adjacent "James" "John" 2] [true :proximity "Jill" "Sally" 1]])


;; (map
;;  (fn [[one two three four :as students]]
;;    (let [students [one two three four]]
;;      {students (generate-seating-plan room students constraints)}))
;; combinations
;;      )
;; => ({["John" "James" "Sally" "Jill"] true}
;;     {["John" "James" "Jill" "Sally"] true}
;;     {["John" "Sally" "James" "Jill"] true}
;;     {["John" "Sally" "Jill" "James"] true}
;;     {["John" "Jill" "James" "Sally"] true}
;;     {["John" "Jill" "Sally" "James"] true}
;;     {["James" "John" "Sally" "Jill"] true}
;;     {["James" "John" "Jill" "Sally"] true}
;;     {["James" "Sally" "John" "Jill"] true}
;;     {["James" "Sally" "Jill" "John"] true}
;;     {["James" "Jill" "John" "Sally"] true}
;;     {["James" "Jill" "Sally" "John"] true}
;;     {["Sally" "John" "James" "Jill"] true}
;;     {["Sally" "John" "Jill" "James"] true}
;;     {["Sally" "James" "John" "Jill"] true}
;;     {["Sally" "James" "Jill" "John"] true}
;;     {["Sally" "Jill" "John" "James"] true}
;;     {["Sally" "Jill" "James" "John"] true}
;;     {["Jill" "John" "James" "Sally"] true}
;;     {["Jill" "John" "Sally" "James"] true}
;;     {["Jill" "James" "John" "Sally"] true}
;;     {["Jill" "James" "Sally" "John"] true}
;;     {["Jill" "Sally" "John" "James"] true}
;;     {["Jill" "Sally" "James" "John"] true})





;; (def room-data-csp
;;   {
;;    ;; :room [[:student nil nil]
;;    ;;        [nil nil :student]
;;    ;;        [:student nil :student]
;;    ;;        [:student nil nil]] ;; Room representation
;;    :domains #{[0 0] [1 2] [2 0] [2 2] [3 0]}  ;; Domains (seats)
;;    :variables ["Sally" "Jill" "James" "John"] ;; Variables (students)
;;    :constraints #{
;;                   (constraint :non-adjacent "James" "John" 2)
;;                   (constraint :proximity "Jill" "Sally" 1)
;;                   }
;;    :assignment
;;    {}
;;    ;; {"Sally" [2 2]
;;    ;;  "Jill" [1 0]
;;    ;;  "James" [3 2]
;;    ;;  "Jack" [1 1]
;;    ;;  "John" [0 1]}
;;    }) ;; Initial assignment

;; => ({["John" "James" "Sally" "Jill"] false}
;;     {["John" "James" "Jill" "Sally"] true}
;;     {["John" "Sally" "James" "Jill"] false}
;;     {["John" "Sally" "Jill" "James"] false}
;;     {["John" "Jill" "James" "Sally"] true}
;;     {["John" "Jill" "Sally" "James"] true}
;;     {["James" "John" "Sally" "Jill"] false}
;;     {["James" "John" "Jill" "Sally"] true}
;;     {["James" "Sally" "John" "Jill"] false}
;;     {["James" "Sally" "Jill" "John"] false}
;;     {["James" "Jill" "John" "Sally"] true}
;;     {["James" "Jill" "Sally" "John"] true}
;;     {["Sally" "John" "James" "Jill"] false}
;;     {["Sally" "John" "Jill" "James"] false}
;;     {["Sally" "James" "John" "Jill"] false}
;;     {["Sally" "James" "Jill" "John"] false}
;;     {["Sally" "Jill" "John" "James"] false}
;;     {["Sally" "Jill" "James" "John"] false}
;;     {["Jill" "John" "James" "Sally"] true}
;;     {["Jill" "John" "Sally" "James"] true}
;;     {["Jill" "James" "John" "Sally"] true}
;;     {["Jill" "James" "Sally" "John"] true}
;;     {["Jill" "Sally" "John" "James"] true}
;;     {["Jill" "Sally" "James" "John"] true})

;;When sally is before jill it is true

;; => ({["John" "James" "Sally" "Jill"] true}
;;     {["John" "James" "Jill" "Sally"] false}
;;     {["John" "Sally" "James" "Jill"] true}
;;     {["John" "Sally" "Jill" "James"] true}
;;     {["John" "Jill" "James" "Sally"] false}
;;     {["John" "Jill" "Sally" "James"] false}
;;     {["James" "John" "Sally" "Jill"] true}
;;     {["James" "John" "Jill" "Sally"] false}
;;     {["James" "Sally" "John" "Jill"] true}
;;     {["James" "Sally" "Jill" "John"] true}
;;     {["James" "Jill" "John" "Sally"] false}
;;     {["James" "Jill" "Sally" "John"] false}
;;     {["Sally" "John" "James" "Jill"] true}
;;     {["Sally" "John" "Jill" "James"] true}
;;     {["Sally" "James" "John" "Jill"] true}
;;     {["Sally" "James" "Jill" "John"] true}
;;     {["Sally" "Jill" "John" "James"] true}
;;     {["Sally" "Jill" "James" "John"] true}
;;     {["Jill" "John" "James" "Sally"] false}
;;     {["Jill" "John" "Sally" "James"] false}
;;     {["Jill" "James" "John" "Sally"] false}
;;     {["Jill" "James" "Sally" "John"] false}
;;     {["Jill" "Sally" "John" "James"] false}
;;     {["Jill" "Sally" "James" "John"] false})


;; (allocation?

;; (generate-seating-plan room students constraints)
;;  )


;; (def example-seats #{[0 1] [1 0] [1 1] [2 2] [3 2]})
;; (def example-students ["Sally" "Jill" "James" "Jack" "John"])

;; (def example-constraints
;;   [[:non-adjacent "James" "John" 1]
;;    [:proximity "Jill" "Sally" 1]
;;   ])

;; (generate-seating-plan example-room example-students example-constraints)
;; ;; => {"James" [2 2], "Sally" [1 0], "Jack" [3 2], "John" [0 1], "Jill" [1 1]}

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; => [96
;;     {:domains #{},
;;      :variables ["Sally" "Jill" "James" "Jack" "John"],
;;      :any-constraints-violated? #object [cljs$core$sp2],
;;      :assignment
;;      {"Sally" [1 0], "Jill" [1 1], "James" [2 2], "Jack" [3 2], "John" [0 1]}}]

;; => [78
;;     {:domains #{},
;;      :variables ["Sally" "Jill" "James" "Jack" "John"],
;;      :any-constraints-violated? #object [cljs$core$sp1],
;;      :assignment
;;      {"Sally" [2 2], "Jill" [3 2], "James" [1 0], "Jack" [1 1], "John" [0 1]}}]

;; => [9
;;     {:domains #{},
;;      :variables ["Sally" "Jill" "James" "Jack" "John"],
;;      :any-constraints-violated? #object [cljs$core$sp2],
;;      :assignment
;;      {"Sally" [2 2], "Jill" [1 0], "James" [3 2], "Jack" [1 1], "John" [0 1]}}]

;; (def room-data
;;   {:room [[:person :desk] [nil :person]]             ;; Room representation
;;    :domains #{[0 1] [1 0] [1 1] [2 2] [3 2]}        ;; Domains (seats)
;;    :variables ["Sally" "Jill" "James" "Jack" "John"] ;; Variables (students)
;;    :constraints #{
;;                   ;; (constraint :non-adjacent "James" "Jack" 1)
;;                   (constraint :non-adjacent "James" "John" 1)
;;                   (constraint :proximity "Jill" "Sally" 1)
;;                 }
;;    :assignment {"Sally" [2 2]
;;                 "Jill" [1 0]
;;                 "James" [3 2]
;;                 "Jack" [1 1]
;;                 "John" [0 1]}}) ;; Initial assignment



;;=====================================
;;Playing Around
;;====================================


;; (def constraints #{(constraint :WA :NT)
;;                    (constraint :WA :SA)
;;                    (constraint :NT :SA)
;;                    (constraint :NT :Q)
;;                    (constraint :SA :Q)
;;                    (constraint :SA :NSW)
;;                    (constraint :SA :V)
;;                    (constraint :Q :NSW)
;;                    (constraint :V :T)})
;; ;; => Execution error (Error) at (<cljs repl>:1).
;; ;;    No method in multimethod 'seatingplanner.views.csp/constraint' for dispatch value: :V
;; ;;    :repl/exception!(apply some-fn constraints)


;; (def variables [:WA :NT :Q :NSW :V :SA :T])
;; (zipmap variables (repeat nil))



;; (def csp (init #{:red :green :blue}
;;             [:WA :NT :Q :NSW :V :SA :T]
;;             #{(constraint :WA :NT)
;;               (constraint :WA :SA)
;;               (constraint :NT :SA)
;;               (constraint :NT :Q)
;;               (constraint :SA :Q)
;;               (constraint :SA :NSW)
;;               (constraint :SA :V)
;;               (constraint :Q :NSW)
;;               (constraint :V :T)})
;; )




;; (def domains #{:red :green :blue})
;; (map (fn [d]
;;        (assoc-in csp
;;                  [:assignment (select-unassigned-variable csp)]
;;                  d))
;;        (:domains csp))
;; ;; => ({:domains #{:green :red :blue},
;; ;;      :variables [:WA :NT :Q :NSW :V :SA :T],
;; ;;      :any-constraints-violated? #object [cljs$core$spn],
;; ;;      :assignment {:WA :green, :NT nil, :Q nil, :NSW nil, :V nil, :SA nil, :T nil}}
;; ;;     {:domains #{:green :red :blue},
;; ;;      :variables [:WA :NT :Q :NSW :V :SA :T],
;; ;;      :any-constraints-violated? #object [cljs$core$spn],
;; ;;      :assignment {:WA :red, :NT nil, :Q nil, :NSW nil, :V nil, :SA nil, :T nil}}
;; ;;     {:domains #{:green :red :blue},
;; ;;      :variables [:WA :NT :Q :NSW :V :SA :T],
;; ;;      :any-constraints-violated? #object [cljs$core$spn],
;; ;;      :assignment {:WA :blue, :NT nil, :Q nil, :NSW nil, :V nil, :SA nil, :T nil}})

;;  (filter (comp nil?
;;                        (partial get-in csp)
;;                        (partial conj [:assignment]))
;;                  (:variables csp))

;; (defn add-five [x] (+ x 5))
;; ;; (defn double [x] (* x 2))
;; (def add-five-and-double (comp double add-five))
;; (add-five-and-double 3)


;; (def numbers (range 1 11)) ; A sequence of numbers from 1 to 10
;; (filter (comp even?
;;               #(mod % 3))
;;           numbers)

;; (def v (:variables csp))
;; (conj [:assignment] (first v))
;; ;; => [:assignment :WA]
;; (get-in csp [:assignment :WA])
;; (nil? nil)

;; (partial get-in csp)
;; (partial conj [:assignments])


;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ;;Next-CSPS

;; (map (fn [d]
;;        (assoc-in csp
;;                  [:assignment (select-unassigned-variable csp)]
;;                  d))
;;      (:domains csp))
;; ;; => ({:domains #{:green :red :blue},
;; ;;      :variables [:WA :NT :Q :NSW :V :SA :T],
;; ;;      :any-constraints-violated? #object [cljs$core$spn],
;; ;;      :assignment {:WA :green, :NT nil, :Q nil, :NSW nil, :V nil, :SA nil, :T nil}}
;; ;;     {:domains #{:green :red :blue},
;; ;;      :variables [:WA :NT :Q :NSW :V :SA :T],
;; ;;      :any-constraints-violated? #object [cljs$core$spn],
;; ;;      :assignment {:WA :red, :NT nil, :Q nil, :NSW nil, :V nil, :SA nil, :T nil}}
;; ;;     {:domains #{:green :red :blue},
;; ;;      :variables [:WA :NT :Q :NSW :V :SA :T],
;; ;;      :any-constraints-violated? #object [cljs$core$spn],
;; ;;      :assignment {:WA :blue, :NT nil, :Q nil, :NSW nil, :V nil, :SA nil, :T nil}})


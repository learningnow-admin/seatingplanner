(ns seatingplanner.views.forms
  (:require
   [reitit.frontend.easy :as rtfe]
   [re-frame.core :as re-frame]
   [reagent.core :as reagent]
   [fork.bulma :as bulma]
   [fork.re-frame :as fork]))

;;==============================
;; FORM ========================
;;==============================


;;==============================
;; ADD CLASS ===================
;;==============================

(defn add-class []
  (let [status @(re-frame/subscribe [:add-class-form-status])]

    [fork/form {:path [:forms]
                :form-id "form-id"
                :prevent-default? true
                :clean-on-unmount? true
                :on-submit #(re-frame/dispatch [:add-class %])
                }
     (fn [{:keys [values
                  form-id
                  handle-change
                  handle-blur
                  handle-submit] :as props}]

       [:form
        {:id form-id
         :on-submit handle-submit}

        [:div.modal {:class (str (if status "is-active" ""))}
         [:div.modal-background]
         [:div.modal-card
          [:header.modal-card-head
           [:p.modal-card-title "Add Class"]
           [:button.delete {:aria-label "close"
                            :on-click #(do
                                         (re-frame/dispatch [:toggle-add-class-form-status])
                                         (.preventDefault %)
                                         )}]]
          [:section.modal-card-body

           [bulma/input props
            {:name "input"
             :label "Class Name"
             :type "text"
             :class ""}]

           [bulma/textarea props
            {:name "area"
             :label "Student Names"
             :type "text"
             :class "your-css-class"}]]
          [:footer.modal-card-foot
           [:div.buttons {:class "buttons"}
            [:button.button.is-success {:type "submit"
                                        } "Create"]
            [:button.button {:class "button"
                             :on-click #(do
                                          (re-frame/dispatch [:toggle-add-class-form-status])
                                          (.preventDefault %))}
             "Cancel"]
            ]]]]])]))



;;==============================
;; ADD STUDENT =================
;;==============================
(defn add-student [class-id active-class-seating-plan-id]
  (let [status @(re-frame/subscribe [:add-student-form-status])]
    [fork/form {:path [:forms-add-student]
                :form-id "add-student"
                :prevent-default? true
                :clean-on-unmount? true
                :on-submit #(re-frame/dispatch [:add-student class-id active-class-seating-plan-id %])
                }
     (fn [{:keys [values
                  form-id
                  handle-change
                  handle-blur
                  handle-submit] :as props}]

       [:form
        {:id form-id
         :on-submit handle-submit}

        [:div.modal {:class (str (if status "is-active" ""))}
         [:div.modal-background]
         [:div.modal-card
          [:header.modal-card-head
           [:p.modal-card-title "Add student"]
           [:button.delete {:aria-label "close"
                            :on-click #(do
                                         (re-frame/dispatch [:toggle-add-student-form-status])
                                         (.preventDefault %)
                                         )}]]
          [:section.modal-card-body
           [bulma/input props
            {:name "input"
             :label "Student Name"
             :type "text"
             :class ""}]
           ]
          [:footer.modal-card-foot
           [:div.buttons {:class "buttons"}
            [:button.button.is-success {:type "submit"
                                        } "Add"]
            [:button.button {:class "button"
                             :on-click #(do
                                          (re-frame/dispatch [:toggle-add-student-form-status])
                                          (.preventDefault %))}
             "Cancel"]
            ]]

          ]]])]))

;;==============================
;; ADD CONSTRAINTS =================
;;==============================





(defn add-constraint [class-id students]
  (let [status @(re-frame/subscribe [:add-constraint-form-status])
        students (vec (concat '({"" "Please select"}) (map #(hash-map % %) students)))]
    [fork/form {:path [:forms-add-constraint]
                :form-id "add-constraint"
                :prevent-default? true
                :clean-on-unmount? true
                :on-submit #(re-frame/dispatch [:add-constraint class-id %])
                }
     (fn [{:keys [values
                  form-id
                  handle-change
                  handle-blur
                  handle-submit] :as props}]

       [:form
        {:id form-id
         :on-submit handle-submit}

        [:div.modal {:class (str (if status "is-active" ""))}
         [:div.modal-background]
         [:div.modal-card
          [:header.modal-card-head
           [:p.modal-card-title "Add constraint"]
           [:button.delete {:aria-label "close"
                            :on-click #(do
                                         (re-frame/dispatch [:toggle-add-constraint-form-status])
                                         (.preventDefault %)
                                         )}]]
          [:section.modal-card-body

           [:div.grid.grid-cols-2 ;;.flex.gap-4.flex-wrap ;;.overflow-auto.gap-4
            [bulma/dropdown props
             {:label "Student 1"
              :name "s1"
              :options students
              :class "w-full"
              }]

            [bulma/dropdown props
             {:label "Student 2"
              :name "s2"
              :options students
              :class "w-full"
              }]

            [bulma/dropdown props
             {:label "Type"
              :name "type"
              :options [{"" "Please select"} {:proximity "Together"}
               {:non-adjacent "Apart"}]
              :class "w-full"
              }]
            [bulma/dropdown props
             {
              :label "Num of Spaces"
              :name "space"
              :options  (vec (concat '({"" "Please select"})
                                     (for [i (range 1 21)]
                                       {i i})))
              :class "w-full"
              }]
            ]
           ]
          [:footer.modal-card-foot
           [:div.buttons {:class "buttons"}
            [:button.button.is-success {:type "submit"
                                        } "Add"]
            [:button.button {:class "button"
                             :on-click #(do
                                          (re-frame/dispatch [:toggle-add-constraint-form-status])
                                          (.preventDefault %))}
             "Cancel"]
            ]]

          ]]])]))




(defn add-layout [class-id]
  (let [status @(re-frame/subscribe [:add-layout-form-status])
        rooms @(re-frame/subscribe [:rooms])
        rooms-options (vec (concat '({"default" "Please select"})
                               (map #(hash-map (first %) (:name (second %))) rooms)) )
                       ]

    ;; (js/alert rooms)
   (if (empty? rooms)
        [:div.modal {:class (str (if status "is-active" ""))}
         [:div.modal-background]
         [:div.modal-card
          [:header.modal-card-head
           [:p.modal-card-title "Add a room first"]
           [:button.delete {:aria-label "close"
                            :on-click #(do
                                         (re-frame/dispatch [:toggle-add-layout-form-status])
                                         (.preventDefault %)
                                         )}
            ]]
          [:section.modal-card-body

           [:div.grid ;;.flex.gap-4.flex-wrap ;;.overflow-auto.gap-4


            [:button.button.is-success {:type "submit"
                                    :on-click #(re-frame/dispatch [:toggle-add-room-form-status])
                                        } "Add a room"]

            ]
           ]

          [:footer.modal-card-foot
           [:div.buttons {:class "buttons"}
            [:button.button {:class "button"
                             :on-click #(do
                                          (re-frame/dispatch [:toggle-add-layout-form-status])
                                          (.preventDefault %))}
             "Cancel"]
            ]]]]


     ;; [:p "mananage"]
     ;; (js/alert rooms-options)
     [fork/form {:path [:forms-add-layout]
                 :form-id "add-layout"
                 :prevent-default? true
                 :clean-on-unmount? true
                 :on-submit
                 ;; #(js/alert "Hello")
                 #(re-frame/dispatch [:add-layout class-id %])
                 }
      (fn [{:keys [values
                   form-id
                   handle-change
                   handle-blur
                   handle-submit] :as props}]

        [:form
         {:id form-id
          :on-submit handle-submit}

         [:div.modal {:class (str (if status "is-active" ""))}
          [:div.modal-background]
          [:div.modal-card
           [:header.modal-card-head
            [:p.modal-card-title "Add Seating Plan"]
            [:button.delete {:aria-label "close"
                             :on-click #(do
                                          (re-frame/dispatch [:toggle-add-layout-form-status])
                                          (.preventDefault %)
                                          )}]]
           [:section.modal-card-body

            [:div.grid ;;.flex.gap-4.flex-wrap ;;.overflow-auto.gap-4
             [bulma/input props
              {:name "name"
               :label "Name"
               :type "text"
               :class ""}]

             [bulma/dropdown props
              {:label "Room"
               :name "room"
               :options rooms-options
               :class ""
               }]



             ;; [:div.grid.grid-cols-2

             ;; [bulma/input props
             ;;  {:name "w"
             ;;   :label "Width"
             ;;   :type "number"
             ;;   :class ""}]

             ;; [bulma/input props
             ;;  {:name "h"
             ;;   :label "Height"
             ;;   :type "number"
             ;;   :class ""}]
             ;;  ]

             ]
            ]
           [:footer.modal-card-foot
            [:div.buttons.flex
             [:button.button.is-success {:type "submit"
                                         } "Add"]
             [:button.button {:class "button"
                              :on-click #(do
                                           (re-frame/dispatch [:toggle-add-layout-form-status])
                                           (.preventDefault %))}
              "Cancel"]

             [:div.flex.justify-end [:button.button {
                                                                     :on-click #(do
                                                                                  (re-frame/dispatch [:toggle-add-room-form-status])
                                                                                  (.preventDefault %))
                                                                     } "Add room"]]
             ]

            ]

           ]]])]
)
    )

   )

;;==============================
;; ADD ROOM ====================
;;==============================

(defn add-room []
  (let [status @(re-frame/subscribe [:add-room-form-status])]

    [fork/form {:path [:forms]
                :form-id "form-id"
                :prevent-default? true
                :clean-on-unmount? true
                :on-submit #(re-frame/dispatch [:add-room %])
                }
     (fn [{:keys [values
                  form-id
                  handle-change
                  handle-blur
                  handle-submit] :as props}]

       [:form
        {:id form-id
         :on-submit handle-submit}

        [:div.modal {:class (str (if status "is-active" ""))
                     }
         [:div.modal-background]
         [:div.modal-card
          [:header.modal-card-head
           [:p.modal-card-title "Add room"]
           [:button.delete {:aria-label "close"
                            :on-click #(do
                                         (re-frame/dispatch [:toggle-add-room-form-status])
                                         (.preventDefault %)
                                         )}]]
          [:section.modal-card-body

           [bulma/input props
            {:name "input"
             :label "Room Name"
             :type "text"
             :class ""}]

            [:div.grid.grid-cols-2

            [bulma/input props
             {:name "w"
              :label "Spaces Wide"
              :type "number"
              :placeholder "10-20"
              :class ""}]

            [bulma/input props
             {:name "h"
              :label "Spaces High"
              :placeholder "10-20"
              :type "number"
              :class ""}]
             ]
            ]


          [:footer.modal-card-foot
           [:div.buttons {:class "buttons"}
            [:button.button.is-success {:type "submit"
                                        } "Add"]
            [:button.button {:class "button"
                             :on-click #(do
                                          (re-frame/dispatch [:toggle-add-room-form-status])
                                          (.preventDefault %))}
             "Cancel"]
            ]]]]])]))

;;==============================
;; COPY ROOM ====================
;;==============================
(defn copy-room []
  (let [status @(re-frame/subscribe [:copy-room-form-status])
        room-id @(re-frame/subscribe [:room-id])
        ]

    [fork/form {:path [:forms]
                :form-id "form-id"
                :prevent-default? true
                :clean-on-unmount? true
                :on-submit #(re-frame/dispatch [:copy-room % room-id])
                }
     (fn [{:keys [values
                  form-id
                  handle-change
                  handle-blur
                  handle-submit] :as props}]

       [:form
        {:id form-id
         :on-submit handle-submit}

        [:div.modal {:class (str (if status "is-active" ""))
                     }
         [:div.modal-background]
         [:div.modal-card
          [:header.modal-card-head
           [:p.modal-card-title "Copy Room"]
           [:button.delete {:aria-label "close"
                            :on-click #(do
                                         (re-frame/dispatch [:toggle-copy-room-form-status])
                                         (.preventDefault %)
                                         )}]]
          [:section.modal-card-body

           [bulma/input props
            {:name "input"
             :label "New Room Name"
             :type "text"
             :class ""}]


            ]


          [:footer.modal-card-foot
           [:div.buttons {:class "buttons"}
            [:button.button.is-success {:type "submit"
                                        } "Add"]
            [:button.button {:class "button"
                             :on-click #(do
                                          (re-frame/dispatch [:toggle-copy-room-form-status])
                                          (.preventDefault %))}
             "Cancel"]
            ]]]]])]))



;;==============================
;; COPY SEATING PLAN ===========
;;==============================
 ;;:toggle-copy-seating-plan-form-status
(defn copy-seating-plan [class-id active-class-seating-plan-id]
  (let [

        status @(re-frame/subscribe [:copy-seating-plan-form-status])
        ;; room-id @(re-frame/subscribe [:room-id])
        ]

    [fork/form {:path [:forms]
                :form-id "form-id"
                :prevent-default? true
                :clean-on-unmount? true
                :on-submit #(re-frame/dispatch [:copy-seating-plan % class-id active-class-seating-plan-id])
                }
     (fn [{:keys [values
                  form-id
                  handle-change
                  handle-blur
                  handle-submit] :as props}]

       [:form
        {:id form-id
         :on-submit handle-submit}

        [:div.modal {:class (str (if status "is-active" ""))
                     }
         [:div.modal-background]
         [:div.modal-card
          [:header.modal-card-head
           [:p.modal-card-title "Copy seating plan"]
           [:button.delete {:aria-label "close"
                            :on-click #(do
                                         (re-frame/dispatch [:toggle-copy-seating-plan-form-status])
                                         (.preventDefault %)
                                         )}]]
          [:section.modal-card-body

           [bulma/input props
            {:name "name"
             :label "New seating plan name"
             :type "text"
             :class ""}]


            ]


          [:footer.modal-card-foot
           [:div.buttons {:class "buttons"}
            [:button.button.is-success {:type "submit"
                                        } "Add"]
            [:button.button {:class "button"
                             :on-click #(do
                                          (re-frame/dispatch [:toggle-copy-seating-plan-form-status])
                                          (.preventDefault %))}
             "Cancel"]
            ]]]]])]))


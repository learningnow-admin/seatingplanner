(ns seatingplanner.stylesgarden
  (:require-macros
    [garden.def :refer [defcssfn]])
  (:require
    [spade.core   :refer [defglobal defclass]]
    [garden.units :refer [deg px]]
    [garden.color :refer [rgba]]
    [garden.core :refer [css]]
    ))

;;===============================================================================
;; GLOBAL PROPERTIES
;;===============================================================================

(defglobal defaults
  [

   ;; [:body {:background-color "red"
   ;;         :margin "0"
   ;;         :padding "0"}

   ;;  ]

   ;; [:td :th {
   ;;           :border "1px solid #dddddd"
   ;;           :text-align "left"
   ;;           :padding "8px"
   ;;           }]
   ;;  [:h1 {
   ;;        :color "blue"
   ;;        }]

    ])

;;===============================================================================
;; LAYOUT SYSTEM
;;===============================================================================
;; (defclass classes-layout []
;;   {
;;    :display "grid"
;;    :grid-template (str "1fr 1fr 1fr 1fr 5px")
;;    :grid-gap "2px"
;;    })

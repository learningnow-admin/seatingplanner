(ns seatingplanner.routes
  (:require
   [re-frame.core :as rf]
   [reitit.coercion.schema :as rsc]
   [reitit.frontend :as rtf]
   [reitit.frontend.easy :as rtfe]
   [seatingplanner.helpers :refer [gdb sdb]]
   [seatingplanner.views.home :as home]
   [seatingplanner.views.rooms :as rooms]
   [seatingplanner.views.room :as room]
   [seatingplanner.views.classes :as classes]
   [seatingplanner.views.class :as class]
   ))

;;https://clojure.org/guides/weird_characters#__code_code_var_quote
(def routes
    (rtf/router
      ["/"
       [""
        {:name :routes/#frontpage
         :view #'home/main}]
       ["rooms"
        {:name :routes/#rooms
         :view #'rooms/main}]

       ["room"
        {:name :routes/#room
         :view #'room/main}]
       ["classes"
        {:name :routes/#classes
         :view #'classes/main}]
       ["class"
        {:name :routes/#class
         :view #'class/main}]
       ]

      {:data {:coercion rsc/coercion}}))

(defn on-navigate [new-match]
  (when new-match
    (rf/dispatch [:routes/navigated new-match])))

(defn app-routes []
  (rtfe/start! routes
               on-navigate
               {:use-fragment true}))

(rf/reg-sub
 :routes/current-route
 (gdb [:current-route]))

;;; Events
(rf/reg-event-db
 :routes/navigated
 (sdb [:current-route]))

(rf/reg-event-fx
 :routes/navigate
 (fn [_cofx [_ & route]]
   {:routes/navigate! route}))





(ns seatingplanner.views.room
  (:require
   [cljs.pprint :as pp]
   [re-frame.core :as re-frame]
   [reagent.core :as reagent]
   [reitit.frontend.easy :as rtfe]
   [seatingplanner.db :as db]
   [seatingplanner.helpers :as h]
   [fontawesome.icons :as icons]
   [fork.bulma :as bulma]
   [fork.re-frame :as fork]
   [seatingplanner.stylesgarden :as gstyle]
   [seatingplanner.toolsview :as vt]
   [seatingplanner.views.editor :as editor]
   ))

(defn layout-rooms []
  (let [
        room-id @(re-frame/subscribe [:room-id])
        room @(re-frame/subscribe [:room room-id])
        ]
    [:div.card.p-4
      [:div.card-header
       [:div.card-header-title (str "Rooms - "(:name room))]
       [:div.card-header-icon
        [:div.px-2
         [:a.button.is-white
         {:href (rtfe/href :routes/#rooms)}
          ;; {:on-click #(re-frame/dispatch [:full-screen-toggle])}
          (icons/render (icons/icon :fontawesome.solid/arrow-left) {:size 20})]
         ]]]
      [:div.card-content
     [editor/editor [:rooms, room-id, :layout] room]

]]

    )
  )



;;      [:div.card

;;        [:div.card-header-icon
;;         [:div.px-2
;;          [:button.button.is-white
;;           {:on-click #(re-frame/dispatch [:full-screen-toggle])}
;;           (icons/render (icons/icon :fontawesome.solid/maximize) {:size 20})]
;;          ]]]
;;       [:div.card-content

;;        [editor path named-layout]
;; ]
;;       [:div.card-footer
;;        ;; [:div.card-footer-item [:p.font-bold "Allocate"]]
;;          [:button.card-footer-item


;;            {:on-click #(re-frame/dispatch [:organise class-id active-class-seating-plan-id])}
;;           [:p
;;            "Allocate"]]
;;          [:button.card-footer-item..bg-red-100.hover:bg-red-500
;;                      {:on-click #(re-frame/dispatch [:delete-layout class-id active-class-seating-plan-id])}
;;           [:p
;;            "Delete"]

;;           ]
;; ]]


















(defn main []
  [:<>
   ;; [:div [:h1.text-xl.text-center "ROOMS"]]
   [layout-rooms]
   ])


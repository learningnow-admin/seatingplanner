(ns seatingplanner.views.rooms
  (:require
   [re-frame.core :as re-frame]
   [reagent.core :as reagent]
   [reitit.frontend.easy :as rtfe]
   [fontawesome.icons :as icons]
   [seatingplanner.helpers :as h]
   [seatingplanner.views.forms :as form]))

(defn room-row [room-id {:keys [name layout]}]
  [:tr
   [:td {:style {:padding "12px 16px"}}
    [:a {:title "Edit the layout of this room"
         :on-click #(re-frame/dispatch [:room-id room-id])
         :href (rtfe/href :routes/#room)}
     [:span {:style {:color "#2563eb" :font-weight "500" :cursor "pointer"}
             :class "hover:underline"}
      name]]]
   [:td {:style {:padding "12px 16px" :color "#6b7280"}}
    (h/vector-dimensions layout)]
   [:td {:style {:padding "12px 16px"}}
    [:div.flex.justify-end {:style {:gap "6px"}}
     [:button.p-1.rounded
      {:style {:color "#9ca3af"}
       :title "Create a copy of this room layout"
       :on-click #(re-frame/dispatch [:toggle-on-copy-room-form-status room-id])}
      (icons/render (icons/icon :fontawesome.solid/copy) {:size 15})]
     [:button.p-1.rounded
      {:style {:color "#f87171"}
       :title "Delete this room"
       :on-click #(when (js/confirm (str "Delete room \"" name "\"? This cannot be undone."))
                    (re-frame/dispatch [:delete-room room-id]))}
      (icons/render (icons/icon :fontawesome.solid/trash) {:size 15})]]]])

(defn rooms []
  (let [rooms @(re-frame/subscribe [:rooms])]
    (if (seq rooms)
      [:div.p-6
       [:div.flex.justify-between.items-center.mb-5
        [:h1.text-2xl.font-bold.text-gray-800 "Rooms"]
        [:button.button.is-primary
         {:title "Add a new room"
          :on-click #(re-frame/dispatch [:toggle-add-room-form-status])}
         [:span.mr-1 "+"] " Add Room"]]

       [:div.rounded-xl.shadow.overflow-hidden.border.border-gray-200
        [:table.table.w-full
         [:thead.bg-gray-50
          [:tr
           [:th.px-4.py-3.text-left.text-sm.font-semibold.text-gray-600 "Room Name"]
           [:th.px-4.py-3.text-left.text-sm.font-semibold.text-gray-600 "Dimensions"]
           [:th ""]]]
         [:tbody
          (for [[room-id room] rooms]
            ^{:key room-id} [room-row room-id room])]]]]

      [:div.flex.flex-col.items-center.justify-center.p-16.text-center
       [:div.text-5xl.mb-4 "🏫"]
       [:h2.text-xl.font-semibold.text-gray-700.mb-2 "No rooms yet"]
       [:p.text-gray-500.mb-6 "Create a room to define where students will sit."]
       [:button.button.is-primary
        {:on-click #(re-frame/dispatch [:toggle-add-room-form-status])}
        "Add Your First Room"]])))

(defn main []
  [:<>
   [rooms]
   [form/add-room]
   [form/copy-room]])

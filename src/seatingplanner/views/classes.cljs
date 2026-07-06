(ns seatingplanner.views.classes
  (:require
   [re-frame.core :as re-frame]
   [reagent.core :as reagent]
   [reitit.frontend.easy :as rtfe]
   [fontawesome.icons :as icons]
   [seatingplanner.views.forms :as form]))

(defn class-row [class-id {:keys [name students constraints]}]
  [:tr
   [:td {:style {:padding "12px 16px"}}
    [:a {:title "Manage seating plans for this class"
         :on-click #(re-frame/dispatch [:class-id class-id])
         :href (rtfe/href :routes/#class)}
     [:span {:style {:color "#2563eb" :font-weight "500" :cursor "pointer"}
             :class "hover:underline"}
      name]]]
   [:td {:style {:padding "12px 16px" :color "#6b7280"}}
    (str (count students) " students")]
   [:td {:style {:padding "12px 16px" :color "#6b7280"}}
    (str (count (filter first constraints)) " rules")]
   [:td {:style {:padding "12px 16px"}}
    [:div.flex.justify-end
     [:button.p-1.rounded
      {:style {:color "#f87171"}
       :title "Delete this class"
       :on-click #(when (js/confirm (str "Delete class \"" name "\"? This cannot be undone."))
                    (re-frame/dispatch [:delete-class class-id]))}
      (icons/render (icons/icon :fontawesome.solid/trash) {:size 15})]]]])

(defn classes []
  (let [classes @(re-frame/subscribe [:classes])]
    (if (seq classes)
      [:div.p-6
       [:div.flex.justify-between.items-center.mb-5
        [:h1.text-2xl.font-bold.text-gray-800 "Classes"]
        [:button.button.is-primary
         {:title "Add a new class"
          :on-click #(re-frame/dispatch [:toggle-add-class-form-status])}
         [:span.mr-1 "+"] " Add Class"]]

       [:div.rounded-xl.shadow.overflow-hidden.border.border-gray-200
        [:table.table.w-full
         [:thead.bg-gray-50
          [:tr
           [:th.px-4.py-3.text-left.text-sm.font-semibold.text-gray-600 "Class Name"]
           [:th.px-4.py-3.text-left.text-sm.font-semibold.text-gray-600 "Students"]
           [:th.px-4.py-3.text-left.text-sm.font-semibold.text-gray-600 "Constraints"]
           [:th ""]]]
         [:tbody
          (for [[class-id class] classes]
            ^{:key class-id} [class-row class-id class])]]]]

      [:div.flex.flex-col.items-center.justify-center.p-16.text-center
       [:div.text-5xl.mb-4 "📋"]
       [:h2.text-xl.font-semibold.text-gray-700.mb-2 "No classes yet"]
       [:p.text-gray-500.mb-6 "Create a class and add your students to get started."]
       [:button.button.is-primary
        {:on-click #(re-frame/dispatch [:toggle-add-class-form-status])}
        "Add Your First Class"]])))

(defn main []
  [:<>
   [classes]
   [form/add-class]])

(ns seatingplanner.views.class
  (:require
   [re-frame.core :as re-frame]
   [fontawesome.icons :as icons]
   [reagent.core :as reagent]
   [reitit.frontend.easy :as rtfe]
   [seatingplanner.views.editor :as editor]
   [seatingplanner.views.forms :as form]))

;;==============================
;;STUDENTS =====================
;;==============================
(defn student-list [class-id student active-class-seating-plan-id]
  [:li
   [:div.flex.items-center.py-1.px-2.gap-2.rounded.hover:bg-white
    {:style {:margin-bottom "2px"}}
    [:span.flex-1.text-sm.text-gray-700 student]
    [:button.text-blue-400.hover:text-blue-600.p-1.rounded
     {:title (str "Add " student " to seating plan")
      :on-click #(re-frame/dispatch [:student-to-seating-plan student active-class-seating-plan-id])}
     (icons/render (icons/icon :fontawesome.solid/plus) {:size 11})]
    [:button.text-gray-300.hover:text-red-500.p-1.rounded
     {:title (str "Remove " student " from class")
      :on-click #(when (js/confirm (str "Remove \"" student "\" from the class?"))
                   (re-frame/dispatch [:delete-student class-id student]))}
     (icons/render (icons/icon :fontawesome.solid/xmark) {:size 11})]]])

(defn sidebar-section [label & children]
  [:div {:style {:margin-bottom "20px"}}
   [:p.menu-label {:style {:margin-bottom "6px"}} label]
   (into [:<>] children)])

(defn students [class-id class active-class-seating-plan-id]
  [sidebar-section (str "Students (" (count (:students class)) ")")
   [:ul.menu-list
    (for [student (:students class)]
      ^{:key student} [student-list class-id student active-class-seating-plan-id])]
   [:div {:style {:margin-top "8px"}}
    [:button.button.is-small.is-light
     {:title "Add a student to this class"
      :on-click #(re-frame/dispatch [:toggle-add-student-form-status])}
     (icons/render (icons/icon :fontawesome.solid/plus) {:size 11})
     [:span.ml-1 "Add student"]]]
   [form/add-student class-id active-class-seating-plan-id]])

;;==============================
;;CONSTRAINTS ==================
;;==============================
(defn constraint->string [[_checked? directive name1 name2 distance]]
  (if (= directive :non-adjacent)
    (str name1 " & " name2 " — more than " distance " spaces apart")
    (str name1 " & " name2 " — within " distance " spaces")))

(defn constraints-list [class-id constraint]
  (let [[checked?] constraint]
    [:li
     [:div.flex.items-start.py-1.px-2.gap-2.rounded.hover:bg-white
      {:style {:margin-bottom "2px"}}
      [:input {:type "checkbox"
               :checked checked?
               :style {:margin-top "3px" :flex-shrink "0"}
               :on-change #(re-frame/dispatch [:toggle-constraint class-id constraint])}]
      [:p.flex-1.text-xs.text-gray-600
       {:style {:line-height "1.4"
                :text-decoration (when-not checked? "line-through")
                :color (when-not checked? "#9ca3af")}}
       (constraint->string constraint)]
      [:button.text-gray-300.hover:text-red-500.p-1.rounded.flex-shrink-0
       {:on-click #(re-frame/dispatch [:delete-constraint class-id constraint])}
       (icons/render (icons/icon :fontawesome.solid/xmark) {:size 10})]]]))

(defn constraints [class-id class]
  [sidebar-section "Seating Rules"
   [:ul.menu-list
    (for [constraint (:constraints class)]
      ^{:key constraint} [constraints-list class-id constraint])]
   (when (empty? (:constraints class))
     [:p.text-xs.text-gray-400.italic.px-2 {:style {:margin-bottom "4px"}} "No rules yet"])
   [:div {:style {:margin-top "8px"}}
    [:button.button.is-small.is-light
     {:on-click #(re-frame/dispatch [:toggle-add-constraint-form-status])
      :title "Add a seating rule (e.g. keep two students apart)"}
     (icons/render (icons/icon :fontawesome.solid/plus) {:size 11})
     [:span.ml-1 "Add rule"]]]])

;;==============================
;;SEATING PLANS ================
;;==============================

(defn get-active-id [data]
  (->> data (filter :active) (map :id) first))

(defn class-m [class-id {:keys [name]} active-class-id]
  [:li
   [:a {:class (if (= active-class-id class-id) "is-active" "")
        :on-click #(re-frame/dispatch [:class-id class-id])}
    name]])

(defn classes-list [active-class-id]
  (let [classes @(re-frame/subscribe [:classes])]
    [sidebar-section "Classes"
     [:ol.menu-list
      (for [[class-id class] classes]
        ^{:key class-id} [class-m class-id class active-class-id])]]))

(defn seating-plan-m [seating-plan-id {:keys [name]} active-seating-plan-id class-id]
  [:li
   [:a {:class (if (= seating-plan-id active-seating-plan-id) "is-active" "")
        :on-click #(re-frame/dispatch [:change-layout class-id seating-plan-id])}
    name]])

(defn seatingplans-list [class-id active-seating-plan-id active-class-seating-plans]
  [sidebar-section "Seating Plans"
   [:ol.menu-list
    (for [[seating-plan-id seating-plan] active-class-seating-plans]
      ^{:key seating-plan-id}
      [seating-plan-m seating-plan-id seating-plan active-seating-plan-id class-id])]
   (when (empty? active-class-seating-plans)
     [:p.text-xs.text-gray-400.italic.px-2 {:style {:margin-bottom "4px"}} "No plans yet"])
   [:div {:style {:margin-top "8px"}}
    [:button.button.is-small.is-light
     {:on-click #(re-frame/dispatch [:toggle-add-layout-form-status])
      :title "Create a seating plan for this class"}
     (icons/render (icons/icon :fontawesome.solid/plus) {:size 11})
     [:span.ml-1 "New plan"]]]
   [form/add-layout class-id]])


(defn layout-classes [class-id class]
  (let [full-screen @(re-frame/subscribe [:full-screen])
        class-seating-plan-ids (:seating-plans class)
        seating-plans @(re-frame/subscribe [:seating-plans])
        active-class-seating-plan-id (get-active-id (:seating-plans class))
        active-class-seating-plans (select-keys seating-plans (vec (map #(:id %) class-seating-plan-ids)))
        seating-plan (get seating-plans active-class-seating-plan-id)
        plan-name (:name seating-plan)]
    [:<>
     (if full-screen
       [:div {:style {:display "grid" :grid-template-columns "220px 1fr"}}

        ;; Left sidebar — fixed width, full page height, with sections
        [:aside.menu.bg-gray-50
         {:style {:padding "16px 12px"
                  :border-right "1px solid #e5e7eb"
                  :min-height "calc(100vh - 52px)"
                  :overflow-y "auto"}}

         [classes-list class-id]

         [seatingplans-list class-id active-class-seating-plan-id active-class-seating-plans]

         [constraints class-id class]
         [form/add-constraint class-id (:students class)]

         [students class-id class active-class-seating-plan-id]]

        ;; Right: editor panel
        (if (not (empty? active-class-seating-plans))
          [:div {:style {:padding "16px"}}
           [editor/complete-editor
            [:seating-plans active-class-seating-plan-id :layout]
            seating-plan
            class-id
            active-class-seating-plan-id]]
          [:div.flex.flex-col.items-center.justify-center.text-center
           {:style {:padding "80px 32px"}}
           [:p.text-5xl {:style {:margin-bottom "16px"}} "📐"]
           [:h2 {:style {:font-size "1.2rem" :font-weight "600" :color "#374151" :margin-bottom "8px"}}
            "No seating plans yet"]
           [:p {:style {:color "#6b7280" :margin-bottom "24px"}}
            "Create a seating plan to start arranging students."]
           [:button.button.is-primary
            {:on-click #(re-frame/dispatch [:toggle-add-layout-form-status])}
            "Create a Seating Plan"]])]

       ;; Compact / minimised card view
       [:div.card {:style {:margin "16px"}}
        [:div.card-header
         [:div.card-header-title plan-name]
         [:div.card-header-icon
          [:button.button.is-white
           {:title "Expand to full editor"
            :on-click #(re-frame/dispatch [:full-screen-toggle])}
           (icons/render (icons/icon :fontawesome.solid/maximize) {:size 18})]]]
        [:div.card-content
         [editor/layout-display
          [:seating-plans active-class-seating-plan-id :layout]
          (:layout seating-plan)]]])]))

;;==============================
;;CLASS  =======================
;;==============================
(defn main []
  (let [class-id @(re-frame/subscribe [:class-id])
        class    @(re-frame/subscribe [:get-class class-id])]
    [:<>
     (if (empty? class)
       [:div.flex.flex-col.items-center.justify-center.text-center
        {:style {:padding "80px 32px"}}
        [:p.text-5xl {:style {:margin-bottom "16px"}} "📋"]
        [:h2 {:style {:font-size "1.2rem" :font-weight "600" :color "#374151" :margin-bottom "8px"}}
         "No class selected"]
        [:p {:style {:color "#6b7280" :margin-bottom "24px"}}
         "Go to Classes and select or create a class first."]
        [:a.button.is-primary {:href (rtfe/href :routes/#classes)}
         "Go to Classes"]]
       [layout-classes class-id class])

     [form/add-class]
     [form/add-room]]))

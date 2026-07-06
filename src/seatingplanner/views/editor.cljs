(ns seatingplanner.views.editor
  (:require
   [clojure.string :as str]
   [re-frame.core :as re-frame]
   [reagent.core :as reagent]
   [reitit.frontend.easy :as rtfe]
   [seatingplanner.db :as db]
   [seatingplanner.helpers :as h]
   [fork.bulma :as bulma]
   [seatingplanner.views.forms :as form]
   [fontawesome.icons :as icons]
   [fork.re-frame :as fork]
   [seatingplanner.stylesgarden :as gstyle]
   [seatingplanner.toolsview :as vt]
   ))

(defn add-rows-top [path]
  [:<>
   [:button.button.is-small
    {:title "Add row at top"
     :on-click #(re-frame/dispatch [:add-row-top path])}
    (icons/render (icons/icon :fontawesome.solid/plus) {:size 16})]
   [:button.button.is-small
    {:title "Remove row from top"
     :on-click #(re-frame/dispatch [:remove-row-top path])}
    (icons/render (icons/icon :fontawesome.solid/minus) {:size 16})]])

(defn add-rows-bottom [path]
  [:<>
   [:button.button.is-small
    {:title "Add row at bottom"
     :on-click #(re-frame/dispatch [:add-row-bottom path])}
    (icons/render (icons/icon :fontawesome.solid/plus) {:size 16})]
   [:button.button.is-small
    {:title "Remove row from bottom"
     :on-click #(re-frame/dispatch [:remove-row-bottom path])}
    (icons/render (icons/icon :fontawesome.solid/minus) {:size 16})]])


(defn add-columns-left [path]
  [:div.grid
   [:button.button.is-small
    {:title "Add column on left"
     :on-click #(re-frame/dispatch [:add-column-left path])}
    (icons/render (icons/icon :fontawesome.solid/plus) {:size 16})]
   [:button.button.is-small
    {:title "Remove column from left"
     :on-click #(re-frame/dispatch [:remove-column-left path])}
    (icons/render (icons/icon :fontawesome.solid/minus) {:size 16})]])

(defn add-columns-right [path]
  [:div.grid
   [:button.button.is-small
    {:title "Add column on right"
     :on-click #(re-frame/dispatch [:add-column-right path])}
    (icons/render (icons/icon :fontawesome.solid/plus) {:size 16})]
   [:button.button.is-small
    {:title "Remove column from right"
     :on-click #(re-frame/dispatch [:remove-column-right path])}
    (icons/render (icons/icon :fontawesome.solid/minus) {:size 16})]])


(def toggle-buttons "px-3 py-2 border border-gray-300 rounded cursor-pointer text-sm font-medium transition-colors")
(def inactive " bg-gray-100 text-gray-600 hover:bg-gray-200")
(def active " ring-2 ring-offset-1")


(defn toggle-spot [path]
  (let [a @(re-frame/subscribe [:toggle-spot])]
    [:div {:style {:padding-top "10px"}}
     [:div.flex.items-center.flex-wrap {:style {:gap "6px"}}
      [:span.text-xs.text-gray-400 {:style {:margin-right "2px"}} "Paint:"]
      [:button
       {:title "Paint empty chair/seat"
        :class (str toggle-buttons
                    (if (= :student a)
                      " bg-yellow-100 border-yellow-400 text-yellow-900"
                      inactive))
        :on-click #(re-frame/dispatch [:toggle-spot :student])}
       "Chair "
       (icons/render (icons/icon :fontawesome.solid/chair) {:size 13})]

      [:button
       {:title "Paint desk/furniture (not a seat)"
        :class (str toggle-buttons
                    (if (= :desk a)
                      " text-white"
                      inactive))
        :style (when (= :desk a) {:background-color "#92400e" :border-color "#78350f"})
        :on-click #(re-frame/dispatch [:toggle-spot :desk])}
       "Desk "
       (icons/render (icons/icon :fontawesome.solid/square) {:size 13})]

      [:button
       {:title "Clear / erase a cell"
        :class (str toggle-buttons
                    (if (= nil a)
                      " bg-gray-300 border-gray-400 text-gray-800"
                      inactive))
        :on-click #(re-frame/dispatch [:toggle-spot nil])}
       "Clear "
       (icons/render (icons/icon :fontawesome.regular/square) {:size 13})]

      [:button
       {:title "Clear the entire grid"
        :style {:margin-left "8px"}
        :class (str toggle-buttons " border-red-200 text-red-400 hover:text-red-600 hover:border-red-400")
        :on-click #(when (js/confirm "Clear the entire grid? This will remove all seats and students.")
                     (re-frame/dispatch [:clear-all path]))}
       (icons/render (icons/icon :fontawesome.solid/trash) {:size 13})]]]))


(defn legend-swatch [color label]
  [:div.flex.items-center {:style {:gap "5px"}}
   [:div {:style {:width "13px" :height "13px" :border-radius "3px"
                  :border "1px solid #d1d5db" :background-color color
                  :flex-shrink "0"}}]
   [:span label]])

(defn legend []
  [:div.flex.flex-wrap.items-center
   {:style {:gap "12px" :margin-top "10px" :font-size "11px" :color "#9ca3af" :padding "0 2px"}}
   [:span {:style {:font-weight "500" :color "#d1d5db"}} "Key:"]
   [legend-swatch "#fef9c3" "Empty seat"]
   [legend-swatch "#bbf7d0" "Student seated"]
   [legend-swatch "#92400e" "Desk"]
   [legend-swatch "#f3f4f6" "Open space"]])


(def item-class
  "border grid place-items-center text-xs font-medium")

;; Cell background colors use inline styles so they always render
;; regardless of whether Tailwind has rebuilt the CSS.
(defn cell-style [spot-value]
  (cond
    (= spot-value :desk)    {:background-color "#92400e" :color "#fff"}       ;; dark brown — desk/furniture
    (= spot-value :student) {:background-color "#fef9c3" :color "#713f12"}    ;; light yellow — empty seat
    (string? spot-value)    {:background-color "#bbf7d0" :color "#14532d"}    ;; light green — student seated
    :else                   {:background-color "#f3f4f6" :color "#6b7280"}))  ;; light gray — empty space

(defn cell [path row column spot-value dragging-id valid-drop-id]
  [:div
   (if (or (string? spot-value) (= spot-value :student) (= spot-value :desk))

     ;; OCCUPIED CELL — draggable
     {:class item-class
      :style (merge (cell-style spot-value) {:cursor "grab"})
      :on-click #(re-frame/dispatch [:change-cell path row column])
      :draggable "true"
      :on-drag-start (fn [_event] (reset! dragging-id [row column]))
      :on-drag-over (fn [event] (.preventDefault event))
      :on-drag-enter #(do (reset! valid-drop-id [row column]) (.preventDefault %))
      :on-drop (fn [_event] (re-frame/dispatch [:swap-cells path @dragging-id @valid-drop-id]))
      :on-drag-end (fn [_event] (reset! dragging-id nil) (reset! valid-drop-id nil))}

     ;; EMPTY CELL — drop target
     {:class item-class
      :style (merge (cell-style spot-value) {:cursor "pointer"})
      :on-click #(re-frame/dispatch [:change-cell path row column])
      :draggable "true"
      :on-drag-over (fn [event] (.preventDefault event))
      :on-drag-enter #(do (reset! valid-drop-id [row column]) (.preventDefault %))
      :on-drop (fn [_event] (re-frame/dispatch [:swap-cells path @dragging-id @valid-drop-id]))
      :on-drag-end (fn [_event] (reset! dragging-id nil) (reset! valid-drop-id nil))})

   (when (string? spot-value)
     [:span.truncate.px-1 {:style {:max-width "60px"}} spot-value])])


(defn layout-display [path layout]
  (let [num-columns (count (first layout))
        num-rows (count layout)
        valid-drop-id (reagent/atom nil)
        dragging-id (reagent/atom nil)]
    [:<>
     [:div
      {:style {:display "grid"
               :grid-template-columns (str "repeat(" num-columns ", minmax(40px, 1fr))")
               :grid-template-rows (str "repeat(" num-rows ", minmax(40px, 1fr))")}}
      (for [row (range num-rows)
            column (range num-columns)]
        (let [spot-value (get-in layout [row column])]
          ^{:key [row column]} [cell path row column spot-value dragging-id valid-drop-id]))]]))


(defn editor [path named-layout]
  (let [layout (:layout named-layout)]
    [:<>
     [:div.content {:style {:display "grid" :grid-template-columns "40px 1fr 1fr 40px"}}
      [:div.col-start-2 [add-rows-top path]]
      [:div.col-start-1 [add-columns-left path]]
      [:div.col-start-2.col-span-2 [layout-display path layout]]
      [:div.col-start-4 [add-columns-right path]]
      [:div.col-start-2 [add-rows-bottom path]]
      [:div.col-start-3 [toggle-spot path]]]
     [legend]]))


(defn spinner []
  [:svg
   {:class "animate-spin -ml-1 mr-3 h-5 w-5 text-white"
    :xmlns "http://www.w3.org/2000/svg"
    :fill "none"
    :viewBox "0 0 24 24"}
   [:circle
    {:class "opacity-25"
     :cx "12"
     :cy "12"
     :r "10"
     :stroke "currentColor"
     :stroke-width "4"}]
   [:path
    {:class "opacity-75"
     :fill "currentColor"
     :d "M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"}]])


(defn validation-panel [result]
  (when result
    (let [all-good? (and (empty? (:missing result))
                         (empty? (:extras result))
                         (:constraints-met? result))]
      [:div.mx-3.mt-2.mb-1.p-3.rounded-lg.text-sm
       {:class (if all-good?
                 "bg-green-50 border border-green-300 text-green-900"
                 "bg-red-50 border border-red-300 text-red-900")}
       [:p.font-semibold.mb-1
        (if all-good? "✓ Seating plan looks good!" "⚠ Issues found:")]
       (when (seq (:missing result))
         [:p.mb-0.5 "Missing students: " [:span.font-medium (str/join ", " (:missing result))]])
       (when (seq (:extras result))
         [:p.mb-0.5.text-orange-700 "Not in class list: " [:span.font-medium (str/join ", " (:extras result))]])
       [:p {:class (if (:constraints-met? result) "text-green-700" "text-red-700")}
        (if (:constraints-met? result)
          "✓ All seating constraints are met"
          "✗ One or more constraints are not satisfied")]])))


(defn complete-editor [path named-layout class-id active-class-seating-plan-id]
  (let [name (:name named-layout)
        spinning? @(re-frame/subscribe [:spinner])
        validation-result @(re-frame/subscribe [:validation-result])]
    [:div.card {:style {:margin "4px"}}
     [:div.card-header
      [:div.card-header-title {:style {:font-size "0.95rem"}}
       (str "Seating Plan: " name)]
      [:div.card-header-icon
       [:button.button.is-white.is-small
        {:title "Minimise editor"
         :on-click #(re-frame/dispatch [:full-screen-toggle])}
        (icons/render (icons/icon :fontawesome.solid/maximize) {:size 16})]]]

     [:div.card-content {:style {:padding "16px"}}
      [editor path named-layout]]

     [validation-panel validation-result]

     [:div.card-footer {:style {:border-top "1px solid #e5e7eb"}}
      [:button.card-footer-item
       {:style {:background-color "#86efac" :font-weight "500"}
        :title "Auto-fill students into seats, respecting all enabled constraints"
        :disabled spinning?
        :on-click #(re-frame/dispatch [:organise class-id active-class-seating-plan-id])}
       (when spinning? [spinner])
       "Autofill "
       (icons/render (icons/icon :fontawesome.solid/wand-magic-sparkles) {:size 14})]

      [:button.card-footer-item
       {:title "Check whether all students are seated and constraints are satisfied"
        :on-click #(re-frame/dispatch [:validate class-id active-class-seating-plan-id])}
       "Validate "
       (icons/render (icons/icon :fontawesome.solid/check) {:size 14})]

      [:button.card-footer-item
       {:title "Make a copy of this seating plan"
        :on-click #(re-frame/dispatch [:toggle-copy-seating-plan-form-status])}
       "Copy "
       (icons/render (icons/icon :fontawesome.solid/copy) {:size 14})]
      [form/copy-seating-plan class-id active-class-seating-plan-id]

      [:button.card-footer-item
       {:style {:color "#ef4444"}
        :title "Delete this seating plan"
        :on-click #(when (js/confirm "Delete this seating plan? This cannot be undone.")
                     (re-frame/dispatch [:delete-layout class-id active-class-seating-plan-id]))}
       "Delete "
       (icons/render (icons/icon :fontawesome.solid/trash) {:size 14})]]]))

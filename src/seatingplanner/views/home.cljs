(ns seatingplanner.views.home
  (:require
   [re-frame.core :as re-frame]
   [reitit.frontend.easy :as rtfe]
   [seatingplanner.toolsview :as vt]))

;;==============================
;; TOAST NOTIFICATION
;;==============================
(defn toast-style [type]
  (case type
    :success {:background "#f0fdf4" :border-left "4px solid #22c55e" :color "#14532d"}
    :error   {:background "#fef2f2" :border-left "4px solid #ef4444" :color "#7f1d1d"}
    :warning {:background "#fffbeb" :border-left "4px solid #f59e0b" :color "#78350f"}
             {:background "#eff6ff" :border-left "4px solid #3b82f6" :color "#1e3a8a"}))

(defn toast []
  (let [notification @(re-frame/subscribe [:notification])]
    (when notification
      [:div {:style {:position "fixed" :bottom "20px" :right "20px" :z-index "50"
                     :max-width "360px" :min-width "260px"
                     :border-radius "8px" :box-shadow "0 4px 12px rgba(0,0,0,0.15)"
                     :overflow "hidden"}}
       [:div {:style (merge (toast-style (:type notification))
                            {:display "flex" :align-items "flex-start"
                             :gap "12px" :padding "14px 16px"})}
        [:p {:style {:flex "1" :font-size "0.875rem" :font-weight "500" :margin "0"}}
         (:message notification)]
        [:button {:style {:background "none" :border "none" :cursor "pointer"
                          :font-size "1.2rem" :line-height "1" :opacity "0.6"
                          :padding "0" :color "inherit"}
                  :on-click #(re-frame/dispatch [:clear-notification (:id notification)])}
         "×"]]])))

;;==============================
;; HOME PAGE
;;==============================
(defn step-card [{:keys [number bg border hover-bg num-bg href title description]}]
  [:a {:href href
       :style {:display "block" :border-radius "12px"
               :border (str "2px solid " border)
               :background bg :padding "20px 24px"
               :text-decoration "none"
               :transition "box-shadow 0.15s, background 0.15s"}}
   [:div {:style {:display "flex" :align-items "center" :gap "16px"}}
    [:div {:style {:width "40px" :height "40px" :border-radius "50%"
                   :background num-bg :color "white"
                   :display "flex" :align-items "center" :justify-content "center"
                   :font-weight "700" :font-size "1.1rem" :flex-shrink "0"}}
     number]
    [:div
     [:h2 {:style {:font-size "1.05rem" :font-weight "600"
                   :color "#1f2937" :margin "0 0 4px 0"}}
      title]
     [:p {:style {:font-size "0.875rem" :color "#6b7280" :margin "0"}}
      description]]]])

(defn main []
  [:div {:style {:display "flex" :justify-content "center"
                 :padding "64px 24px"}}
   [:div {:style {:width "100%" :max-width "560px"}}

    [:div {:style {:text-align "center" :margin-bottom "48px"}}
     [:h1 {:style {:font-size "2.2rem" :font-weight "700"
                   :color "#111827" :margin "0 0 10px 0"}}
      "Seating Planner"]
     [:p {:style {:font-size "1.05rem" :color "#6b7280" :margin "0"}}
      "Design classroom seating arrangements with ease"]]

    [:div {:style {:display "flex" :flex-direction "column" :gap "16px"}}
     [step-card {:number      "1"
                 :bg          "#eff6ff"
                 :border      "#bfdbfe"
                 :num-bg      "#2563eb"
                 :href        (rtfe/href :routes/#rooms)
                 :title       "Create Rooms"
                 :description "Draw your classroom layout — place seats, desks and walkways on a grid"}]
     [step-card {:number      "2"
                 :bg          "#faf5ff"
                 :border      "#e9d5ff"
                 :num-bg      "#7c3aed"
                 :href        (rtfe/href :routes/#classes)
                 :title       "Create Classes"
                 :description "Add your class roster and set seating rules between students"}]
     [step-card {:number      "3"
                 :bg          "#f0fdf4"
                 :border      "#bbf7d0"
                 :num-bg      "#16a34a"
                 :href        (rtfe/href :routes/#class)
                 :title       "Create Seating Plans"
                 :description "Auto-fill students into seats or arrange them manually with drag & drop"}]]]])

;;==============================
;; ROUTING / SHELL
;;==============================
(def toolbar-items
  [["Home"          :routes/#frontpage]
   ["Rooms"         :routes/#rooms]
   ["Classes"       :routes/#classes]
   ["Seating Plans" :routes/#class]])

(defn show-panel [route]
  (when-let [view (get-in route [:data :view])]
    [view]))

(defn main-panel []
  (let [active-route (re-frame/subscribe [:routes/current-route])]
    [:<>
     [:nav {:style {:background "white"
                    :border-bottom "1px solid #e5e7eb"
                    :box-shadow "0 1px 3px rgba(0,0,0,0.05)"
                    :padding "0 24px"
                    :height "52px"
                    :display "flex"
                    :align-items "center"
                    :justify-content "space-between"}}
      [:a {:href  (rtfe/href :routes/#frontpage)
           :style {:text-decoration "none" :display "flex" :align-items "center"}}
       [:img {:src "favicon.png" :alt "Learning Now" :style {:height "32px"}}]]
      [vt/navigation toolbar-items @active-route]]

     [show-panel @active-route]
     [toast]]))

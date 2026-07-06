(ns seatingplanner.toolsview
  (:require [reitit.frontend.easy :as rtfe]))


(defn item [e]
  (cond
     (fn? e) [e]
     (vector? e) e
     (string? e) [:h2 e]))

(defn panel [name component]
  [:div

   [item name]
   [item component]])

;; navigation tools
(defn nav-item [i current-route]
  (if (= :sep i)
    [:span {:style {:color "#d1d5db" :margin "0 2px"}} "|"]
    (let [route-name (second i)
          active? (= route-name (get-in current-route [:data :name]))]
      [:a {:href (rtfe/href route-name)
           :style (if active?
                    {:color "#1d4ed8" :font-weight "600"
                     :border-bottom "2px solid #2563eb"
                     :padding-bottom "2px" :text-decoration "none"}
                    {:color "#6b7280" :text-decoration "none"
                     :font-size "0.9rem"})}
       (first i)])))

(defn navigation [routes current-route]
  (let [coll (->> routes (interpose :sep) (map-indexed vector))]
    [:div {:style {:display "flex" :align-items "center" :gap "12px"}}
     (for [[idx rt] coll]
       ^{:key (str idx)} [nav-item rt current-route])]))

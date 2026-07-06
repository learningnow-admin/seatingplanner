(ns ^:figwheel-hooks seatingplanner.core
  (:require
   [goog.dom :as gdom]
   [reagent.dom :as rdom]
   [re-frame.core :as re-frame]
   [react :as react]
   [reagent.dom.client :as rdc]
   [seatingplanner.config :as config]
   [seatingplanner.events]
   [fork.re-frame]
   ;; [seatingplanner.subs]
   [seatingplanner.routes :as routes]
   [seatingplanner.styles :as styl]
   [seatingplanner.views.home :as views]))


(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))
;; (defonce root (rdc/create-root (.getElementById js/document "app")))


(defn ^:dev/after-load mount-root []
  (re-frame/clear-subscription-cache!)
  (styl/inject-trace-styles js/document)
  (let [

        root-el (.getElementById js/document "app")
        ;; root (rdc/create-root root-el)

        ]
    ;; ;; Changed: Render the component tree using .render on the root
    ;; (rdc/render root [:> react/StrictMode {} [#'views/main-panel]])

    (rdom/unmount-component-at-node root-el)
    (rdom/render [:> react/StrictMode {} [#'views/main-panel]]  root-el)
    ))

(defn ^:export init []
  (println "init again..")
  ;; (re-frame/dispatch-sync [::ccases/initialize-db])
  (re-frame/dispatch-sync [:initialize-db])

  (dev-setup)
  (routes/app-routes)

  (mount-root))

;;=============================
;;ORIGINAL ====================
;;=============================
;; (defn dev-setup []
;;   (when config/debug?
;;     (enable-console-print!)
;;     (println "dev mode")))

;; (defonce root (rdc/create-root (gdom/getElement "app")))

;; (defn mount-root []
;;   (println "mount")
;;   (re-frame/clear-subscription-cache!)
;;   (styl/inject-trace-styles js/document)
;;   (rdc/render root [:> react/StrictMode {} [#'views/main-panel]]))

;; (defn ^:after-load re-render []
;;   (mount-root))

;; (defn ^:export init []
;;   (println "init again..")
;;   ;; (re-frame/dispatch-sync [::ccases/initialize-db])
;;   (re-frame/dispatch-sync [:initialize-db])

;;   (dev-setup)
;;   (routes/app-routes)

;;   (mount-root))

;;===========================
;;LEIN PROJECT ==============
;;===========================
;; (ns fork-test.core
;;   (:require
;;    [reagent.dom :as rdom]
;;    [re-frame.core :as re-frame]
;;    [fork-test.events :as events]
;;    [fork-test.views :as views]
;;    [fork-test.config :as config]
;;    ))


;; (defn dev-setup []
;;   (when config/debug?
;;     (println "dev mode")))

;; (defn ^:dev/after-load mount-root []
;;   (re-frame/clear-subscription-cache!)
;;   (let [root-el (.getElementById js/document "app")]
;;     (rdom/unmount-component-at-node root-el)
;;     (rdom/render [views/main-panel] root-el)))

;; (defn init []
;;   (re-frame/dispatch-sync [::events/initialize-db])
;;   (dev-setup)
;;   (mount-root))

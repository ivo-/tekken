;; TODO:
;;
;; - delete old answers
;; - generate variants
;; - generate answer-sheets
;; - generate answer-keys
;;
;; - generate checkui
;; - generate statistics
;;
;; Someday:
;;
;; - diagrams
;; - image recognition
;; - generate printable html file (print view)
;;

(ns tekken.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require  [tekken.util :as util]
             [tekken.verification :as verification]
             [om.core :as om :include-macros true]
             [om.dom :as dom :include-macros true]
             [cljs.core.async :as async :refer [put! <! >! chan map>]]))

;; ============================================================================
;; Data

(def template
  (.-innerText (util/$ "#template")))

(defonce data
  (atom {:test-data {:title "Clojure II"
                     :questions {}
                     :variants 5
                     :per-variant 15}}))

;; ============================================================================
;; Components

(defn options
  "Options component."
  [app owner]
  (reify
    om/IInitState
    (init-state
     [_]
     {:variants (:variants app)
      :per-variant (:per-variant app)

      :onTitle
      (fn [e]
        (om/update! app :title (.. e -target -value)))

      :onChange
      (fn [k e]
        (let [value (.. e -target -value)]
          (if (js/isNaN value)
            (om/set-state! owner k "")
            (do
              (om/set-state! owner k value)
              (om/update! app k value)))))
      :onBlur
      (fn [k e]
        (om/set-state! owner k (k @app)))})

    om/IRenderState
    (render-state
     [_ {:keys [per-variant variants onChange onBlur onTitle]}]
     (dom/form
      #js {:id "options"
           :action "javascript: void(0);"}

      (dom/label nil "Title: ")
      (dom/input
       #js {:type "text"
            :value (:title app)
            :onChange onTitle})

      (dom/label nil "Number of variants: ")
      (dom/input
       #js {:type "text"
            :value variants
            :onBlur (partial onBlur :variants)
            :onChange (partial onChange :variants)})

      (dom/label nil "Questions per variant: ")
      (dom/input
       #js {:type "text"
            :value per-variant
            :onBlur (partial onBlur :per-variant)
            :onChange (partial onChange :per-variant)})))))

(defn editor
  [{:keys [test-data] :as app} owner]
  (reify
    om/IInitState
    (init-state
     [_]
     {:text template
      :onChange
      (fn [_]
        (let [value (->> (om/get-node owner "text")
                         (.-value))]
          (om/set-state! owner :text value)
          (om/update! test-data :questions (util/md->edn value))))})

    om/IDidMount
    (did-mount
     [_]
     ((om/get-state owner :onChange)))

    om/IRenderState
    (render-state
     [_ {:keys [text onChange]}]
     (dom/form
      #js {:action "javascript: void(0);"
           :id "editor"}
      (dom/hr nil)
      (dom/textarea
       #js {:id "code"
            :ref "text"
            :value text
            :onChange onChange})
      (dom/hr nil)))))

(defn preview-button
  "Preview button component."
  [{:keys [test-data]} owner]
  (reify
    om/IInitState
    (init-state
     [_]
     {:onClick
      (fn [_]
        (let [ch (util/html->canvases)]
          (go
           (let [canvases (<! ch)]
             (aset js/window "location" (util/canvases->pdf canvases))))))

      :onMouseEnter
      (fn [_]
        #_(let [ch (util/html->canvases)]
          (go
           (let [canvases (<! ch)
                 node (om/get-node owner "preview")]
             (aset node "innerHTML" "")
             (doall (map #(.appendChild node %) canvases))))))

      :onMouseLeave
      (fn [_]
        #_(-> (om/get-node owner "preview")
            (aset "innerHTML" "")))})

    om/IRenderState
    (render-state
     [_ {:keys [onClick onMouseEnter onMouseLeave]}]
     (dom/div
      nil
      (dom/a #js {:href "javascript:void(0);"
                  :onClick onClick
                  :onMouseEnter onMouseEnter
                  :onMouseLeave onMouseLeave} "Download")
      (dom/div #js {:ref "preview"
                    :className "preview"})))))

(defn answers-key
  "For teachers."
  [app owner]
  (reify
    om/IRender
    (render
     [_]
     (dom/section
      #js {:id "answers-key"
           :className "answers"}
      "Anwers key"))))

(defn answers-sheet
  "For students."
  [app owner]
  (reify
    om/IRender
    (render
     [_]
     (dom/section
      #js {:id "answers-sheet"
           :className "answers"}
      "Anwers sheet"))))

(defn viewer
  "Test viewer component."
  [{:keys [test-data]} owner]
  (reify
    om/IRender
    (render
     [_]
     (dom/div
      #js {:id "viewer"}
      (dom/section
       (clj->js {:ref "page"
                 :className "page"
                 :dangerouslySetInnerHTML
                 {:__html (util/edn->html test-data)}}))
      (om/build answers-key test-data)
      (om/build answers-sheet test-data)))))

(defn home-ui
  "Home page ui."
  [app owner]
  (reify
    om/IRenderState
    (render-state
     [_ {:keys [ch]}]
     (dom/section
      #js {:className "home"}
      (dom/div
       #js {:id "left"}
       (om/build editor app)
       (om/build options (:test-data app))
       (om/build preview-button app))
      (om/build viewer app)))))

(defn tekken
  "The big boss that builds all the components together."
  []
  (om/root
   (fn [app owner]
     (dom/div
      nil
      (om/build home-ui app)))
   data
   {:target (. js/document (getElementById "main"))}))

(tekken)

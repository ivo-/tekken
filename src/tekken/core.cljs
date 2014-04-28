(ns tekken.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require  [tekken.util :as util]
             [tekken.verification :refer [verification]]
             [tekken.viewer :refer [viewer]]
             [om.core :as om :include-macros true]
             [om.dom :as dom :include-macros true]
             [cljs.core.async :as async :refer [put! <! >! chan map>]]))

;; ============================================================================
;; Data

(def template
  (.-innerText (js/document.getElementById "template")))

(def data
  (atom {:test-data {:title "UI тест"
                     :questions {}
                     :variants 5
                     :per-variant 15}
         :variants []
         :solutions []}))

(defn test-data->variants
  "Generate variants from provided app state."
  [{:keys [questions per-variant variants] :as state}]
  (->> #(->> questions
             (map-indexed identity)
             (shuffle)
             (take per-variant)
             (vec))
       (repeatedly variants)
       (vec)))

;; ============================================================================
;; Components

(defn options
  "Component for editing test options."
  [{:keys [test-data] :as app} owner]
  (reify
    om/IInitState
    (init-state
     [_]
     {:variants (:variants test-data)
      :per-variant (:per-variant test-data)

      :onTitle
      (fn [e]
        (om/update! test-data :title (.. e -target -value)))

      :onChange
      (fn [k e]
        (let [value (.. e -target -value)]
          (if (js/isNaN value)
            (om/set-state! owner k "")
            (do
              (om/set-state! owner k value)
              (om/update! test-data k value)))))
      :onBlur
      (fn [k e]
        (om/set-state! owner k (k @test-data)))})

    om/IRenderState
    (render-state
     [_ {:keys [per-variant variants onChange onBlur onTitle onVariantChange]}]
     (dom/form
      #js {:id "options"
           :action "javascript: void(0);"}

      (dom/div #js {:className "field-group"}
               (dom/label nil "Title: ")
               (dom/input
                #js {:type "text"
                     :value (:title test-data)
                     :onChange onTitle}))

      (dom/div #js {:className "field-group"}
               (dom/label nil "Брой варианти: ")
               (dom/input
                #js {:type "text"
                     :value variants
                     :onBlur (partial onBlur :variants)
                     :onChange (partial onChange :variants)}))

      (dom/div #js {:className "field-group"}
               (dom/label nil "Въпроси по вариант: ")
               (dom/input
                #js {:type "text"
                     :value per-variant
                     :onBlur (partial onBlur :per-variant)
                     :onChange (partial onChange :per-variant)}))))))

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
                         (.-value))
              edn (util/md->edn value)]
          (om/set-state! owner :text value)
          (om/update! test-data :questions edn)))})

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

(defn download-button
  "Button for generating and downloading of all pdf files."
  [app owner]
  (reify
    om/IInitState
    (init-state
     [_]
     {:onClick
      (fn [_]
        (om/update! app :variants (-> (:test-data @app)
                                    (test-data->variants))))})

    om/IDidUpdate
    (did-update
      [_ _ _]
      (when (pos? (count (:variants app)))
        (let [ch (util/build)]
          (go
           (js/alert (<! ch))
           (om/update! app :variants [])))))

    om/IRenderState
    (render-state
      [_ {:keys [onClick]}]
      (dom/div
        nil
        (dom/a #js {:href "javascript:void(0);"
                    :onClick onClick} "Download")
        (om/build tekken.viewer/variants app)))))

(defn tekken
  "The big boss that builds all the components together."
  []
  (om/root
    (fn [app owner]
      (dom/section
        nil
        (om/build verification app)
        (dom/div
          #js {:id "left"}
          (om/build options app)
          (om/build editor app)
          (om/build download-button app))
        (om/build viewer app)))
   data
   {:target (. js/document (getElementById "main"))}))

;; ==================================================================
;; Render

(tekken)

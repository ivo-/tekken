;; STATISTICS:
;;
;; - allow dirct data paste
;; - print key
;;
;; TODO:
;;
;; - delete old answers
;;
;; Design:
;;
;; - answer-sheets
;; - answer-keys
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

(def data
  (atom (if-let [d (util/store "app")]
          (assoc d :solutions [(verification/new-solution d)])
          {:render-data nil

           :test-data {:title "Clojure II"
                       :questions {}
                       :variants 5
                       :per-variant 15}
           :variants []
           :solutions []})))

(defn test-data->variants
  [{:keys [questions per-variant variants] :as state}]
  (->> #(->> questions
             (map-indexed identity)
             (shuffle)
             (take per-variant)
             (vec))
       (repeatedly variants)
       (vec)))

(defn variant->data
  [{:keys [variants test-data] :as state} n]
  (assoc
    (->> (nth variants n)
         (mapv (:questions test-data))
         (assoc test-data :questions))
    :variant
    n))

;; ============================================================================
;; Components

(defn options
  "Options component."
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
        (om/set-state! owner k (k @test-data)))

      :onVariantChange
      (fn [e]
        (let [v (.val (js/$ (.. e -target)))]
          (if (= v "–")
            (om/update! app :render-data false)
            (om/update! app :render-data (variant->data @app v)))))})

    om/IRenderState
    (render-state
     [_ {:keys [per-variant variants onChange onBlur onTitle onVariantChange]}]
     (dom/form
      #js {:id "options"
           :action "javascript: void(0);"}

      (dom/label nil "Title: ")
      (dom/input
       #js {:type "text"
            :value (:title test-data)
            :onChange onTitle})

      (dom/label nil "Брой варианти: ")
      (dom/input
       #js {:type "text"
            :value variants
            :onBlur (partial onBlur :variants)
            :onChange (partial onChange :variants)})

      (dom/label nil "Въпроси по вариант: ")
      (dom/input
       #js {:type "text"
            :value per-variant
            :onBlur (partial onBlur :per-variant)
            :onChange (partial onChange :per-variant)})

      (dom/label nil "Покажи вариант: ")
      (apply dom/select
             #js {:onChange onVariantChange
                  :value (:variant (or (:render-data app)
                                       {:variant "–"}))}
             (map #(dom/option #js {:value %} %)
                  (->> (:variants test-data)
                       (range 0)
                       (cons "–"))))))))

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

          ;; Good enough for the demo
          (om/transact! app (fn [s]
                              (let [v (-> (assoc-in s [:test-data :questions] edn))]
                                (->> (test-data->variants (:test-data v))
                                     (assoc-in v [:variants])))))))})

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
  [{:keys [test-data] :as app} owner]
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
  [{:keys [questions variant] as data} owner]
  (reify
    om/IRender
    (render
     [_]
     (dom/section
      #js {:id "answers-key"
           :className "answers"}
      (apply
        dom/div #js {:className "row"}
        (dom/h3 nil (str "Answers sheet"
                         (and variant (str " for variant " variant))))
        (map-indexed
          (fn [index {:keys [answers]}]
            (apply
              dom/div
              #js {:className "column"}
              (dom/div #js {:className "number"} (inc index))
              (map  #(dom/div #js {:className (if % "filled")}) answers)))
          questions))))))

(defn answers-sheet
  "For students."
  [{:keys [questions variant per-variant] :as data} owner]
  (reify
    om/IRender
    (render
     [_]
     (dom/section
      #js {:id "answers-sheet"
           :className "answers"}
      (dom/iframe #js {:src (str "patternRecognition/answersheet.html?"
                                 "answers=" per-variant "&"
                                 "variant=" variant)})))))

(defn viewer
  "Test viewer component."
  [{:keys [test-data render-data] :as app} owner]
  (reify
    om/IDidUpdate
    (did-update
     [_ _ _]
     (util/store "app" app))

    om/IRender
    (render
     [_]
     (let [data (or render-data test-data)]
       (dom/div
        #js {:id "viewer"}
        (dom/section
         (clj->js {:ref "page"
                   :className "page"
                   :dangerouslySetInnerHTML
                   {:__html (util/edn->html data)}}))
        (om/build answers-key data)
        (om/build answers-sheet data))))))

(defn home-ui
  "Home page ui."
  [app owner]
  (reify
    om/IRenderState
    (render-state
     [_ {:keys [ch]}]
     (dom/section
      #js {:className "home"}
      (om/build verification/verification app)
      (dom/div
       #js {:id "left"}
       (om/build editor app)
       (om/build options app)
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

;; ==================================================================
;; Render

(tekken)

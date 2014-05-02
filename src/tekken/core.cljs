(ns tekken.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require  [tekken.util :as util]
             [om.core :as om :include-macros true]
             [om.dom :as dom :include-macros true]
             [cljs.core.async :as async :refer [put! <! >! chan map>]]))

;; ============================================================================
;; Data

(def data
  (atom {:test-data {:title "Програмиране с Clojure, Тест № 1"
                     :questions {}
                     :num-variants 5
                     :per-variant 15}
         :variants []
         :solutions []}))

(def initial-text
  (->> "template"
    (js/document.getElementById)
    (.-innerText)))

(defn test-data->variants
  "Generate variants from provided app state."
  [{:keys [questions per-variant num-variants] :as state}]
  (->> #(->> questions
          (map-indexed identity)
          (shuffle)
          (take per-variant)
          (vec))
    (repeatedly num-variants)
    (vec)))

;; ============================================================================
;; Components

;; ----------------------------------------------------------------------------
;; Editing

(defn options
  "Component responsible for editing test options."
  [{:keys [test-data] :as app} owner]
  (reify
    om/IInitState
    (init-state
      [_]
      {:num-variants (:num-variants test-data)
       :per-variant (:per-variant test-data)

       :onTitleChange
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
      [_ {:keys [per-variant num-variants
                 onChange onBlur onTitleChange]}]
      (dom/form #js {:id "options"
                     :action "javascript: void(0);"}
        (dom/div #js {:className "field-group"}
          (dom/label nil "Title: ")
          (dom/input
            #js {:type "text"
                 :value (:title test-data)
                 :onChange onTitleChange}))

        (dom/div #js {:className "field-group"}
          (dom/label nil "Number of variants: ")
          (dom/input
            #js {:type "text"
                 :value num-variants
                 :onBlur (partial onBlur :num-variants)
                 :onChange (partial onChange :num-variants)}))

        (dom/div #js {:className "field-group"}
          (dom/label nil "Questions per variant: ")
          (dom/input
            #js {:type "text"
                 :value per-variant
                 :onBlur (partial onBlur :per-variant)
                 :onChange (partial onChange :per-variant)}))))))
(defn editor
  "Component responsible for editing markdown for questions."
  [{:keys [test-data] :as app} owner]
  (reify
    om/IInitState
    (init-state
      [_]
      {:text initial-text
       :onChange
       (fn [_]
         (let [value (->> (om/get-node owner "text")
                       (.-value))
               edn (util/parse-md value)]
           (om/set-state! owner :text value)
           (om/update! test-data :questions edn)))})

    om/IDidMount
    (did-mount
      [_]
      ((om/get-state owner :onChange)))

    om/IRenderState
    (render-state
      [_ {:keys [text onChange]}]
      (dom/form #js {:id "editor"
                     :action "javascript: void(0);"}
        (dom/hr nil)
        (dom/textarea #js {:id "code"
                           :ref "text"
                           :value text
                           :onChange onChange})
        (dom/hr nil)))))

;; ----------------------------------------------------------------------------
;; Viewer

(defn draw-squares
  [columns rows add-number? add-label?]
  (->> (map (fn [index]
              (apply
                dom/div #js {:className "column"}
                (when add-label?
                  (dom/div #js {:className "number"} (inc index)))
                (repeatedly rows #(dom/div nil (when add-number? index)))))
         (range columns))
    (apply dom/div #js {:className "row"})))

(defn answers-sheet
  [_ owner {:keys [questions variant per-variant] :as data}]
  (reify
    om/IRender
    (render
     [_]
     (dom/section
       #js {:className "answers-sheet answers"}
       (dom/div
         #js {:className "mark"}
         (dom/div #js {:className "marker top-left"})
         (dom/div #js {:className "marker top-center"})
         (dom/div #js {:className "marker top-right"}))

       (dom/h1 nil "ANSWER SHEET")
       (dom/h4 #js {:className "boxname"} "Name:")

       (dom/h3 nil "Faculty number:" )
       (draw-squares 10 5 true false)

       (dom/h3 nil "Answers:")
       (draw-squares per-variant 4 false true)

       (dom/div
         #js {:className "mark"}
         (dom/div #js {:className "marker bottom-left"})
         (dom/div #js {:className "marker bottom-right"}))))))

(defn answers-key
  [_ owner {:keys [questions variant] :as data}]
  (reify
    om/IRender
    (render
      [_]
      (dom/section
        #js {:className "answers-key answers"}
        (apply
          dom/div #js {:className "row"}
          (dom/h1 nil "ANSWERS KEY" (dom/br nil) variant)
          (map-indexed
            (fn [index {:keys [answers]}]
              (apply
                dom/div #js {:className "column"}
                (dom/div #js {:className "number"} (inc index))
                (map #(dom/div #js {:className (when % "filled")}) answers)))
            questions))))))

(defn test-header
  [{:keys [title variant]}]
  (dom/div
    #js {:className "test-header"}
    (dom/h1 nil title)
    (dom/h2 nil (str "Вариант: " variant))
    (dom/h3 nil (util/today))
    (dom/i #js {:className "fn"} "Факултетен номер:")
    (dom/i nil "Име:")))

(defn answers-table
  []
  (dom/table #js {:className "answers"}
    (apply
      dom/tr nil
      (for [i (range 1 16)] (dom/td nil i)))
    (apply
      dom/tr nil
      (for [i (range 1 16)]
        (dom/td (clj->js {:dangerouslySetInnerHTML
                          {:__html "&nbsp;"}}))))

    (apply
      dom/tr nil
      (for [i (range 16 31)] (dom/td nil i)))

    (apply
      dom/tr nil
      (for [i (range 1 16)]
        (dom/td (clj->js {:dangerouslySetInnerHTML
                          {:__html "&nbsp;"}}))))))

(defn viewer
  "Test viewer component for imediate preview of document state."
  [{:keys [test-data]} owner]
  (reify
    om/IRender
    (render
      [_]
      (let [data (assoc test-data :variant "[example]")]
        (dom/div
          #js {:id "viewer"}
          (dom/section
            #js {:className "page"}
            (test-header test-data)
            (answers-table)
            (dom/div (clj->js {:dangerouslySetInnerHTML
                               {:__html (util/->html data)}})))
          ;; Don't forget to pass some app state to each component. Otherwise
          ;; Om cannot know when to update it.
          (om/build answers-key test-data {:opts data})
          (om/build answers-sheet test-data {:opts data}))))))

;; ----------------------------------------------------------------------------
;; Download

(defn variants
  "Component that renders all the variants with their corresponding
   answers-keys and answers-sheets. This will generate DOM that will
   be used for generating PDFs."
  [{:keys [variants test-data] :as app} owner]
  (reify
    om/IRender
    (render
      [_]
      (apply dom/div
        #js {:id "variants"}
        (for [n (-> (:variants app)
                  (count)
                  (range))]
          (let [data (-> (->> (nth variants n)
                           (mapv (:questions test-data))
                           (assoc test-data :questions))
                       (assoc :variant n))]
            (dom/div
              #js {:className "variant"}
              (dom/section
                (clj->js {:className "page"
                          :dangerouslySetInnerHTML
                          {:__html (util/->html data)}}))
              (om/build answers-sheet nil {:opts data})
              (om/build answers-key nil {:opts data}))))))))

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
            (<! ch)
            ;; TODO: Variants should stay in some form if the user wants to use
            ;; our verification system.
            (om/update! app :variants [])))))

    om/IRenderState
    (render-state
      [_ {:keys [onClick]}]
      (dom/div nil
        (dom/a #js {:href "javascript:void(0);"
                    :onClick onClick} "Download")
        (om/build variants app)))))


(defn tekken
  "The big boss that builds all the components together."
  []
  (om/root
    (fn [app owner]
      (dom/section nil
        (dom/div #js {:id "left"}
          (om/build options app)
          (om/build editor app)
          (om/build download-button app))
        (om/build viewer app)))
    data
    {:target (. js/document (getElementById "main"))}))

;; ==================================================================
;; Render

(tekken)

(ns tekken.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require  [tekken.util :as util]
             [om.core :as om :include-macros true]
             [om.dom :as dom :include-macros true]
             [cljs.core.async :as async :refer [put! <! >! chan map>]]))

;; ============================================================================
;; Data

(def data
  (atom {:test-data {:type :simple
                     :title "Clojure Programming, Quiz № 01"
                     :questions {}
                     :per-variant 15
                     :num-variants 5}
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
         (om/set-state! owner k (k @test-data)))

       :onTypeChange
       (fn [e]
         (let [v (.val (js/$ (.. e -target)))]
           (case v
             "simple"
             (om/update! test-data :type :simple)

             "auto"
             (om/update! test-data :type :auto))))})

    om/IRenderState
    (render-state
      [_ {:keys [per-variant num-variants
                 onChange onBlur onTitleChange onTypeChange]}]
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
                 :onChange (partial onChange :per-variant)}))

        (dom/div #js {:className "field-group"}
          (dom/label nil "Test type: ")
          (dom/select #js {:onChange onTypeChange
                           :value (name (:type test-data))}
            (dom/option #js {:value "simple"} "simple")
            (dom/option #js {:value "auto"} "auto")))))))

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
;; Answers sheet/key

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
  [{:keys [questions variant per-variant] :as data} owner]
  (reify
    om/IRender
    (render
     [_]
     (dom/section
       #js {:className "answers-page answers-sheet"}
       (dom/div
         #js {:className "mark"}
         (dom/div #js {:className "marker top-left"})
         (dom/div #js {:className "marker top-center"})
         (dom/div #js {:className "marker top-right"}))

       (dom/h1 nil "Answers Sheet")
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
  [{:keys [questions variant] :as data} owner]
  (reify
    om/IRender
    (render
      [_]
      (dom/section
        #js {:className "answers-page answers-key"}
        (apply
          dom/div #js {:className "row"}
          (dom/h1 nil "Answers Key")
          (dom/h2 nil variant)
          (map-indexed
            (fn [index {:keys [answers]}]
              (apply
                dom/div #js {:className "column"}
                (dom/div #js {:className "number"} (inc index))
                (map #(dom/div #js {:className (when % "filled")}) answers)))
            questions))))))

;; ----------------------------------------------------------------------------
;; Answers table

(defn answers->letter
  "Returns letter for the correct answer. `coll` is a vector of
   boolean values, one of which is true."
  [coll]
  (cond
    (nth coll 0) "a"
    (nth coll 1) "b"
    (nth coll 2) "c"
    (nth coll 3) "d"
    (nth coll 4) "e"))

(defn answers-table
  "Generates answers table from provide test data and optionally
   includes the correct answers."
  [{:keys [questions per-variant]} include-answers?]
  (let [on-row 15

        rows
        (->> (map :answers questions)
          (map answers->letter)
          (partition-all on-row))

        build-row
        (fn [i row]
          (vector
            ;; Add questions numbers.
            (apply dom/tr nil
              (for [n (range 1 (inc (count row)))]
                (dom/td nil  (+ (* on-row i) n))))

            ;; Add answers or blank cells.
            (if include-answers?
              (apply dom/tr nil
                (for [letter row] (dom/td nil letter)))
              (apply dom/tr nil
                (for [_ row]
                  (dom/td (clj->js {:dangerouslySetInnerHTML
                                    {:__html "&nbsp;"}})))))))]
    (apply dom/table #js {:className "answers-table"}
      (apply concat (map-indexed build-row rows)))))

;; ----------------------------------------------------------------------------
;; Viewer

(defn test-header
  "Generates test header from provided test data."
  [{:keys [title variant]}]
  (dom/div
    #js {:className "test-header"}
    (dom/h1 nil title)
    (dom/h2 nil (str "Variant " variant))
    (dom/h3 nil (util/today))))

(defn test-view
  "Generates test view from provided test data. Here is where we
   decide what to for different test types."
  [data]
  (dom/div nil
    ;; Make sure seaction with class `page` wraps test header and
    ;; contents. On build each section with class page will be split
    ;; into multiple A4 pages if necessary.
    ;;
    ;; Answers shouldn't be in `page` sections.
    (dom/section #js {:className "page"}
      (test-header data)

      (case (:type data)
        :simple
        (dom/div nil
          (dom/i #js {:className "fn"} "FN:")
          (dom/i nil "Name:")
          (answers-table data false))

        :auto
        nil) ;; Om just skips nil values.

      (dom/div (clj->js {:dangerouslySetInnerHTML
                         {:__html (util/->html data)}})))

    ;; Make sure all answers documents are wrapped into
    ;; `section.answers-page`. The builder will include all of them
    ;; into the generated pdf.
    (case (:type data)
      :simple
      (dom/section #js {:className "answers-page"}
        (test-header data)
        (answers-table data true))

      :auto
      (dom/div nil
        ;; Both things are wrapped with `seciton.answers-page`.
        (om/build answers-key data)
        (om/build answers-sheet data)))))

(defn viewer
  "Test viewer component for immediate preview of the document state."
  [{:keys [test-data]} owner]
  (reify
    om/IRender
    (render
      [_]
      (let [data (assoc test-data :variant "_")]
        (dom/div
          #js {:id "viewer"}
          (test-view data))))))

(defn variants
  "Component that renders all the variants with their corresponding
   answers parts. This will render the DOM that will be used for
   generating PDFs."
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
              (test-view data))))))))

;; ----------------------------------------------------------------------------
;; Download

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

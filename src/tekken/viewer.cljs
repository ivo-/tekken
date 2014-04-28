(ns tekken.viewer
  (:require  [tekken.util :as util]
             [om.core :as om :include-macros true]
             [om.dom :as dom :include-macros true]))

(defn generate-test-data-for-variant
  "Generate test-date like data structure for describing a
   variant. The only difference between the two is that test-data
   doesn't have :variant key."
  [{:keys [variants test-data] :as state} n]
  (assoc
    (->> (nth variants n)
         (mapv (:questions test-data))
         (assoc test-data :questions))
    :variant n))

(defn draw-squares [columns rows inside-number? label?]
  (apply dom/div #js {:className "row"}
   (map-indexed
    (fn [index _]
      (apply
       dom/div #js {:className "column"}
       (when label? (dom/div #js {:className "number"} (inc index)))
       (map #(dom/div nil (if inside-number? index)) (range rows))))
    (range columns))))

(defn answers-key
  "For teachers."
  [_ owner {:keys [questions variant] :as data}]
  (reify
    om/IRender
    (render
      [_]
      (dom/section
       #js {:className "answers-key answers"}
       (apply
        dom/div #js {:className "row"}
        (dom/h1 nil (str "ANSWERS KEY"
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
      (dom/h3 nil "Fill your faculty number:" )
      (draw-squares 10 5 true false)
      (dom/h3 nil "Please carefully fill your answers:")
      (draw-squares per-variant 4 false true)
      (dom/div
       #js {:className "mark"}
       (dom/div #js {:className "marker bottom-left"})
       (dom/div #js {:className "marker bottom-right"}))))))

(defn variants
  "Component that renders all the variants with their corresponding
   answers-keys and answer-sheets. This will be used for DOM preparation
   before generating PDFs."
  [app owner]
  (reify
    om/IRender
    (render
      [_]
      (apply dom/div
        #js {:id "variants"}
        (for [v (-> (:variants app)
                  (count)
                  (range))]
          (let [data (generate-test-data-for-variant app v)]
            (dom/div
              #js {:className "variant"}
              (dom/section
                (clj->js {:className "page"
                          :dangerouslySetInnerHTML
                          {:__html (util/edn->html data)}}))
              (om/build answers-sheet app {:opts data})
              (om/build answers-key app {:opts data}))))))))

(defn viewer
  "Test viewer component for imediate preview of current document
   state."
  [app owner]
  (reify
    om/IRender
    (render
     [_]
     (dom/div
       #js {:id "viewer"}
       (dom/section
         (clj->js
           {:id "page"
            :dangerouslySetInnerHTML
            {:__html (util/edn->html (:test-data app))}}))))))

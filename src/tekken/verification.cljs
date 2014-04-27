(ns tekken.verification
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :as async :refer [put! <! >! chan map>]]))

;; ==================================================================
;; Generators

(defn new-answer
  []
  {:value ""})

(defn gen-answers
  [n]
  (mapv new-answer (range n)))

(defn new-solution
  [{:keys [test-data]}]
  {:id ""
   :variant 1
   :answers (gen-answers (:per-variant test-data))})

;; ==================================================================
;; Complements

(def letter->num
  #(case %
    "a" 1
    "b" 2
    "c" 3
    "d" 4
    "e" 5
    ""))

(def num->letter
  #(case %
    1 "a"
    2 "b"
    3 "c"
    4 "d"
    5 "e"
    ""))

(defn answer-input
  [answer owner]
  (reify
    om/IInitState
    (init-state
     [_]
     {:text (:value answer)

      :onBlur
      (fn [e]
        (om/set-state! owner :text (:value @answer)))

      :onChange
      (fn [e]
        (let [v (.. e -target -value)]
          (if (re-find #"^[A-Ea-e]$" v)
            (do
              (om/set-state! owner :text (letter->num v))
              (om/update! answer :value (letter->num v)))
            (om/set-state! owner :text ""))))})

    om/IRenderState
    (render-state
     [_ {:keys [text onChange onBlur]}]
      (dom/input
       #js {:type "text"
            :value (num->letter text)
            :onBlur onBlur
            :onChange onChange}))))

(defn solution-form
  [{:keys [id variant answers] :as solution}
   owner
   {:keys [ch
           variants-count
           questions-count]}]
  (reify
    om/IInitState
    (init-state
      [_]
     {:onIdChange
      (fn [e]
        (let [v (.. e -target -value)]
          (om/update! solution :id v)))

      :onVariantChange
      (fn [e]
        (let [v (.val (js/$ (.. e -target)))]
          (om/update! solution :variant v)))

      :onSolutionComplete
      (fn [e]
        (put! ch :next))

      :onVerificationComplete
      (fn [e]
        (put! ch :end))})

    om/IRenderState
    (render-state
     [_ {:keys [onIdChange onVariantChange onSolutionComplete onVerificationComplete]}]
     (dom/div
      nil
      (dom/div
       nil
       (dom/i nil "Студент: ")
       (dom/input #js {:type "text"
                       :value id
                       :onChange onIdChange}))

      (dom/i nil "Вариант: ")
      (apply dom/select
             #js {:onChange onVariantChange
                  :value variant}
             (map #(dom/option #js {:value %} %)
                  (range 1 (inc variants-count))))

      (dom/div nil "Отговори: ")
      (apply dom/div nil
             (om/build-all answer-input
                           answers))

      (dom/button
       #js {:onClick onSolutionComplete}
       "Следващ")

      (dom/button
       #js {:onClick onVerificationComplete}
       "Край")))))

(defn verification
  [{:keys [solutions
           test-data] :as app} owner]
  (reify
    om/IInitState
    (init-state
      [_]
     {:ch (chan)
      :show false})

    om/IWillMount
    (will-mount
     [_]

     (let [{:keys [ch]} (om/get-state owner)]
       (go-loop
        []
        (case (<! ch)
          :next
          (om/transact! solutions #(conj % (new-solution @app)))

          :end
          (do
            (om/set-state! owner :show false)
            (->> @solutions
                 (butlast)
                 (mapv (fn [student]
                         (-> student
                             (update-in [:answers]
                                        (partial mapv :value)))))
                 (pr-str)
                 (.log js/console))))
        (recur))))

    om/IRenderState
    (render-state
     [_ {:keys [ch show]}]
     (dom/div
      #js {:id "verification"}

      (if show
        (dom/div
         nil
         (dom/span nil "Проверени студенти: ")
         (dom/b nil (count solutions))
         (om/build solution-form
                   (last solutions)
                   {:opts {:ch ch

                           :variants-count
                           (:variants test-data)

                           :questions-count
                           (:per-variant test-data)}}))
        (dom/a
         #js {:href "javascript:void(0);"
              :onClick #(do (om/set-state! owner :show true)
                          (om/update! solutions [(new-solution @app)]))}
         "Започни въвеждане на решения"))))))





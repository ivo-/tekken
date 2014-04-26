(ns tekken.verification
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :as async :refer [put! <! >! chan map>]]))

;; ==================================================================
;; State

(def app-state
  (atom
   {:test-data {:questions [{:text "Q1"
                             :answers [true false false false]}
                            {:text "Q2"
                             :answers [false true false false]}
                            {:text "Q3"
                             :answers [false false true false]}
                            {:text "Q4"
                             :answers [false false false true]}]
                :per-variant 4
                :variants 5
                :title "Тест по clojure"}
    :variants [[1 2 3 4]
               [2 3 4 1]]
    :user-solutions []}))

;; ==================================================================
;; Generators

(defn new-answer
  []
  {:value ""})

(defn gen-name
  []
  (apply str (take 5 (repeatedly #(rand-int 2)))))

(defn gen-answers
  [n]
  (mapv new-answer (range n)))

(defn new-solution
  [{:keys [test-data]}]
  {:id (gen-name)
   :variant 1
   :answers (gen-answers (:per-variant test-data))})

(swap! app-state update-in [:user-solutions] conj (new-solution @app-state))

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
        ;; Note: is it valid state?
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
       (dom/i nil "Student:")
       (dom/input #js {:type "text"
                       :value id
                       :onChange onIdChange}))

      (apply dom/select
             #js {:onChange onVariantChange
                  :value variant}
             (map #(dom/option #js {:value %} %)
                  (range 1 (inc variants-count))))

      (apply dom/div nil
             (om/build-all answer-input answers))

      (dom/button
       #js {:onClick onSolutionComplete}
       "Next")

      (dom/button
       #js {:onClick onVerificationComplete}
       "End")))))

(defn verification
  [{:keys [user-solutions
           test-data] :as app} owner]
  (reify
    om/IInitState
    (init-state
      [_]
     {:ch (chan)})

    om/IWillMount
    (will-mount
     [_]
      (let [{:keys [ch]} (om/get-state owner)]
        (go-loop
         []
         (case (<! ch)
           :next
           (om/transact! user-solutions #(conj % (new-solution @app)))

           :end
           (js/alert "fasfdsfd"))
         (recur))))

    om/IRenderState
    (render-state
     [_ {:keys [ch]}]
     (dom/div
      nil
      (om/build solution-form
                (last user-solutions)
                {:opts {:ch ch

                        :variants-count
                        (:variants test-data)

                        :questions-count
                        (:per-variant test-data)}})))))

(om/root
 verification
 app-state
 {:target (.getElementById js/document "verification")})

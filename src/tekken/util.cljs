(ns tekken.util
  (:require [cljs.core.async :refer [chan put!]]
            [cljs.reader :as reader]))

(defn build
  []
  (let [ch (chan)]
    (js/tekken_build #(put! ch %))
    ch))

(defn md->html
  [s]
  (. js/Markdown toHTML s))

(defn md->edn
  "...where all the magic happens."
  [s]
  (let [prefixes ["a)" "b)" "c)" "d)" "e)" "f)" "g)"]

        hack-rx (js/RegExp. "\\[!]")
        correct-rx (js/RegExp. "^\\+\\s" "gm")

        pre-process-option
        (fn [i [li text & more]]
          (apply vector li
                 (str (prefixes i)
                      " "
                      (if (string? text)
                        (.replace text hack-rx "")
                        text))
                 more))

        pre-process-data
        (fn [[tag & content :as all]]
          (case tag
            "ul"
            (apply vector tag (map-indexed pre-process-option content))

            all))

        parse-question
        (fn [i data]
          {:text
           [(apply
             vector
             "header"
             (let [[tag & more] (first data)]
               (apply vector tag ["b" ["u" (str " " (inc i) ".")] " "] more))
             (mapv pre-process-data (rest data)))]

           :answers
           (->> data
                (filter #(= (first %) "ul"))
                (first)
                (rest)
                (map second)
                (map #(.test hack-rx %)))})]
    (->> (.replace s correct-rx "- [!]")
         (. js/Markdown toHTMLTree)
         (js->clj)
         (rest)
         (partition-by #(= "ul" (first %)))
         (partition-all 2)
         (map (partial reduce concat))
         (map-indexed parse-question)
         (vec))))

(defn edn->html
  [{:keys [questions title variant]}]
  (->> (map :text questions)
       (reduce concat)
       (cons ["i" (str "Вариант: " variant)])
       (cons ["i" "Група:......"])
       (cons ["i" "ФН:......"])
       (cons ["i" "Име:..........................................................................................."])
       (cons ["h1" title])
       (cons "html")
       (clj->js)
       (.renderJsonML js/Markdown)))

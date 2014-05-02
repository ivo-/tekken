(ns tekken.util
  (:require [cljs.core.async :refer [chan put!]]
            [cljs.reader :as reader]))

(defn build
  "Builds the zip and sends it for download."
  []
  (let [ch (chan)]
    (js/tekken_build #(put! ch %))
    ch))

(defn parse-md
  "Parses questions markdown and generates vector of
   questions."
  [s]
  (let [prefixes ["a)" "b)" "c)" "d)" "e)" "f)" "g)"]

        rhack    (js/RegExp. "\\[!]")
        rcorrect (js/RegExp. "^\\+\\s" "gm")

        pre-process-option
        (fn [i [li text & more]]
          (apply vector li
            (str (prefixes i)
              " "
              (if (string? text)
                (.replace text rhack "")
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
           (mapv pre-process-data data)

           :answers
           (->> data
             (filter #(= (first %) "ul"))
             (first)
             (rest)
             (map second)
             (map #(.test rhack %)))})]
    (->> (.replace s rcorrect "- [!]")
      (. js/Markdown toHTMLTree)
      (js->clj)
      (rest) ; remove "html"
      (partition-by #(= "ul" (first %)))
      (partition-all 2)
      (map (partial reduce concat))
      (map-indexed parse-question)
      (vec))))

(defn- add-question-number
  [i data]
  [(apply
     vector
     "header"
     (let [[tag & more] (first data)]
       (apply vector tag ["b" ["u" (str " " (inc i) ".")] " "] more))
     (vec (rest data)))])

(defn today
  []
  (let [moment (js/Date.)
        dd (-> (.getDate moment))
        dd (if (< dd 10) (str "0" dd) dd)
        mm (inc (.getMonth moment))
        mm (if (< mm 10) (str "0" mm) mm)
        yy (.getFullYear moment)]
    (str dd "." mm "." yy "г.")))

(defn ->html
  "Generates HTML. Expects following options:

    :title     - text title
    :variant   - variant name
    :questions - result from parse-md"
  [{:keys [questions title variant]}]
  (->> (map :text questions)
    (map-indexed add-question-number)
    (reduce concat)
    (cons "html")
    (clj->js)
    (.renderJsonML js/Markdown)))

(ns tekken.util
    (:require [cljs.core.async :refer [chan put!]]))

(defn $
  [x]
  (if (string? x)
    (js/document.querySelector x)
    x))

(defn html->canvas
  [node]
  (let [node ($ node)
        ch (chan)]
    (js/html2canvas
     node
     #js {:onrendered #(put! ch %)})
    ch))

(defn canvas->pdf
  [cvs]
  (let [data-url (. cvs toDataURL "image/jpeg")
        pdfdoc  (js/jsPDF. "p" "mm" "a4")]
    (. pdfdoc addImage data-url 0 0 210 297)
    (. pdfdoc output "datauristring")))

(defn make-zip
  []

;;   function genzip()
;;   {
;;    var zip = new JSZip();
;;    zip.file("Hello.txt", "Hello World\n");
;;    var img = zip.folder("images");
;;    /* img.file("smile.jpg", imgData, {base64: true}); */
;;    var content = zip.generate();
;;    location.href="data:application/zip;base64,"+content;
;;    }

  )

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
        (fn [data]
          {:text
           (mapv pre-process-data data)

           :ansers
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
         (map parse-question))))




(def e (md->edn "
Напишете някви лайна?

```
(def a 10)
```

- a
- b
+ `fasfasfsf`
- d

Напишете някви лайна?

```
(def a 10)
```

- a
- b
+ c
- d
"))

(defn edn->html
  [data]
  (->> (map :text data)
       (reduce concat)
       (cons "html")
       (clj->js)
       (.renderJsonML js/Markdown)))

(edn->html e)

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

(defn md->html
  [s]
  (. js/Markdown toHTML s))

(defn md->edn
  [s]
  (let [parse
        (fn [data]
          {:test data
           :ansers
           (->> data
                (filter #(= (first %) "ul"))
                (first)
                (rest)
                (map #(vector (second %) false)))})]
    (->> s
         (. js/Markdown toHTMLTree)
         (js->clj)
         (rest)
         (partition-by #(= ["hr"] %))
         (remove #(= [["hr"]] %))
         (map parse))))

(md->edn "
Напишете някви лайна?

```
(def a 10)
```

- a
- b
+ c
- d

---
")

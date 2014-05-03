;;; TODO: Optional persistence for all user generated tests.

(ns tekken.store
  (:require [cljs.core.async :refer [chan put!]]
            [cljs.reader :as reader]))

(defn store
  ([ns] (store ns nil))
  ([ns edn]
    (if-not (nil? edn)
      (.setItem js/localStorage ns (pr-str edn))
      (let [s (.getItem js/localStorage ns)]
        (when-not (nil? s)
          (reader/read-string s))))))

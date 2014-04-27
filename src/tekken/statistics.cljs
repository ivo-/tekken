(ns tekken.statistics)

(defn single-results [list1 list2]
  (let [n (count list1)]
    (vec (for [x (range n)]
           (if (= (nth list1 x) (nth list2 x))
             true
             false)))))

(defn transform [l]
  (reduce + (for [element l]
              (if element 1 0))))

(defn student-results [statistics-data]
  (let [k (statistics-data :key)]
      (for [student (statistics-data :data)]
        (let [single-result (single-results (student :answers)
                              (nth k (- (student :variant) 1)))]
          {:id (student :id)
           :variant (student :variant)
           :answers single-result
           :points (transform single-result)}))))

(defn average-points [statistics-data]
  (let [l (student-results statistics-data)
        n (count l)]
    {:average-points (/ (reduce + (for [student l]
                                    (student :points))) n)}))

(defn max-points [statistics-data]
  (let [l (student-results statistics-data)]
    (last (sort-by :points l))))

(defn min-points [statistics-data]
  (let [l (student-results statistics-data)]
    (first (sort-by :points l))))

(defn variant-answers [statistics-data]
  (let [variants (-> (statistics-data :key) count)
        k (statistics-data :key)]
    (for [n (range variants)
          student (statistics-data :data)
          :when (= (+ 1 n) (student :variant))]
      (conj (single-results (student :answers)
              (nth k (- (student :variant) 1))) (+ n 1)))))

(defn bool-to-int [l]
  (for [x l]
    (if x 1 0)))

(defn sum [l]
  (reduce + l))

(defn transpose [m]
  (apply mapv vector m))

(defn answers-by-variant [statistics-data]
  (let [data (group-by last (variant-answers statistics-data))
        data1 (for [variant data]
                (transpose (second variant)))
        data2 (for [l (map pop data1)]
                (map #(bool-to-int %) l))
        data3 (for [l data2]
                (map #(sum %) l))]
    (zipmap (map #(+ 1 %) (range (count data3))) data3)))

(def results (atom {}))

(defn global-answers [statistics-data]
  (let [answers (answers-by-variant statistics-data)]
      (for [variant (keys answers)
            global (range (count (statistics-data :variants)))
            :when (= (+ 1 global) variant)]
        (let [current-variant (nth (statistics-data :variants) global)
              number-for-current (answers variant)]
          (for [i (-> (statistics-data :variants) first count range)]
            (if (contains? @results (nth current-variant i))
              (swap! results update-in [(nth current-variant i)] + (nth number-for-current i))
              (swap! results assoc (nth current-variant i) (nth number-for-current i))))))))

(defn all-statistics [statistics-data]
    {"Student Results" (student-results statistics-data)
     "Average Points" (average-points statistics-data)
     "Max Points" (max-points statistics-data)
     "Min Points" (min-points statistics-data)
     "Answers by Variants" (answers-by-variant statistics-data)
     "Answeres by question number" (last (flatten (global-answers statistics-data)))})

;; ==================================================================
;; Example

;; (def statistics-data
;;    "Данните които се подават при проверка на решенията и правене на статистика.
;;     Key е масив от правилните отговори за всички варианти.
;;     Data са всички попълнени тестове, които искаме да проверим"
;;    {:variants [[6 7 3] [2 6 3] [1 7 4]]
;;     :key  [[1 2 3] [1 1 1] [2 3 1]]
;;     :data [{:id 1
;;             :variant 1
;;             :answers [1 3 1]}

;;            {:id 2
;;             :variant 2
;;             :answers [1 3 1]}

;;            {:id 3
;;             :variant 1
;;             :answers [1 2 3]}

;;            {:id 4
;;             :variant 3
;;             :answers [2 3 2]}

;;            {:id 5
;;             :variant 3
;;             :answers [2 3 1]}

;;            {:id 6
;;             :variant 3
;;             :answers [1 3 1]}]})

;; (all-statistics statistics-data)

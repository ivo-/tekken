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

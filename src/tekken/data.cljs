(ns tekken.data)

(def test-data
  "Данните описващи input-a на клиента"
  (atom {:questions [{:text 'Целия текст за ренд'
                      :answers [true false ..]}
                     ...]
         :per-variant 15
         :variants 5
         :title "Тест по clojure"}))

(def variant-data
  "Варианта се представя като съвкупност от въпроси.
   Id-тата на въпросите са зададени имплицитно от наредбата в (test-data :questions)."
  (atom [question-id, question-id, ...]))

(def variant-solutions
  "Ключа с отговорите за вариант е вектор от числа.
   Всяко число оказва верният отговор на въпрос с номер индекса на числото във вектора."
  [answer-question-1 answer-question-2 answer-question-3 ...])

(def user-solution-data
  "Данните представящи един попълнен тест съдържат id определящо човека попълнил теста,
   варианта на теста и масив от числа представящ номера на отговора който е избрал за всеки въпрос."
  (atom {:id 1
         :variant 1
         :answers [1 1 3 1 ...]}) )

(def statistics-data
  "Данните които се подават при проверка на решенията и правене на статистика.
   Key е масив от правилните отговори за всички варианти.
   Data са всички попълнени тестове, които искаме да проверим"
  (atom {:key  [variant-solutions ...]
         :data [user-solution-data ...]}))
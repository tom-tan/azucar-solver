#!/usr/bin/clojure
(use '[clojure.contrib.duck-streams :only (reader)]
     '[clojure.contrib.seq-utils :only (find-first)]
     '[clojure.contrib.error-kit :only (throw-if-not)])

(defn validate [csp ans]
  (let [lines (->> ans reader line-seq)]
    (if-let [result (find-first #(re-find #"^s SATISFIABLE$" %) lines)]
      (let [assignment (read-assignment lines)]
        (if (satisfies csp assignment)
          (prn "ok.")
          (prn "Invalid.")))
      (prn "UNSAT. It can not validate."))))

(defn read-assignment [lines]
  (letfn [(add-assign [a [var val]]
            (assoc a var
                   (cond
                    (re-find #"\d+" val) (BigInteger. val)
                    (= val "true") true
                    (= val "false") false
                    :else (throw (Exception. (str "Invalid value for " var
                                                  ": " val))))))]
    (reduce add-assign {}
            (re-seq #"^a\s+([^\s]+)\s+([^\s]+)$" lines))))

(defn satisfies [csp assign]
  (do-seq [exp (->> csp reader )]
          (throw-if-not (= (eval exp) true))))

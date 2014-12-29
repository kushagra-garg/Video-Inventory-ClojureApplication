(ns  hello-seesaw.backend)


;;;; Reading and Writing data

(def movie-in-store (atom []))
(def rented-movie-in-store (atom []))

(reset! rented-movie-in-store (read-string (slurp "rentedstuff.txt")))
(reset! movie-in-store (read-string (slurp "stuff.txt")))

(defn add-movie-db
  [movie price qty id]
  (->>(assoc-in @movie-in-store [(count @movie-in-store)] {:movie movie :price price :copies qty :id id})
      (reset! movie-in-store)
      (spit "stuff.txt")))

(defn edit-movie-db
 ([movie price qty id row]
  (->>(assoc-in @movie-in-store [row] {:movie movie :price price :copies qty :id id})
      (reset! movie-in-store)
      (spit "stuff.txt")))
 ([row movie-count]
  (->>(update-in @movie-in-store [row :copies] movie-count)
       (reset! movie-in-store)
       (spit "stuff.txt"))))


(defn remove-movie-db
  [row qty?]
   (if(true? qty?)
    (->> (update-in @movie-in-store [row :copies] dec)
         (reset! movie-in-store)
         (spit "stuff.txt"))
    (->> (vec (filter #(not= (nth @movie-in-store row) %) @movie-in-store))
         (reset! movie-in-store)
         (spit "stuff.txt"))))


(defn add-renter-db
  [movie renter date]
   (->> (assoc-in @rented-movie-in-store [(count @rented-movie-in-store)]  {:movie movie :renter renter :date date })
        (reset! rented-movie-in-store)
        (spit "rentedStuff.txt")))

(defn remove-renter-db [row]
  (->> (vec (filter #(not= (nth @rented-movie-in-store row) %) @rented-movie-in-store))
       (reset! rented-movie-in-store)
       (spit "rentedStuff.txt")))

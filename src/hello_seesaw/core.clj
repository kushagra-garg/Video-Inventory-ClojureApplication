(ns  hello-seesaw.core
  (:use seesaw.core
        [clojure.string :only [join]]
        [seesaw core font border table mig])
  (require [seesaw.bind :as bind]
           [hello-seesaw.backend :as backend]
           [clj-time.core :as t]
           [clj-time.format :as tf])
  (refer hello-seesaw.backend)
  (:import [javax.swing JOptionPane JTable]
           javax.swing.table.DefaultTableModel
           java.awt.event.MouseEvent
           java.util.Formatter
           java.util.Date
           java.util.Calendar
           java.awt.BorderLayout)
  (:gen-class))

(native!)

;; Declare variables

(declare make-movie-table)
(declare available-movies-tab)
(declare score-table)
(declare make-renter-table)

;; date format

(def my-format (tf/formatter "MMM d, yyyy 'at' hh:mm:ss"))

;; get value of max id

(def i (atom 0))
(reset! i (last (sort (map #(get % :id) @movie-in-store))))

;; validating dialog OK click

(defn validate [s]
  (when-not (empty? s) s))

;;Price and qty fields validation

(defn validation [e]
  (if (not (or (< 47 (.getKeyCode e) 58) (= 8 (.getKeyCode e)) (= 18 (.getKeyCode e))))
   (let [z (alert "Enter Numeric values")
         x (config! e :text "")])))

;; dialog boxes

(defn return-movie-panel []
  (show! (dialog :content "Do you want to return the movie?"
                 :width 300
                 :height 150
                 :option-type :ok-cancel
                 :success-fn  (fn [p] (str "ok")))))


(defn make-add-movie-panel []
   (show! (dialog :title "Add New Movies" :width 400
                  :id :make-add-movie-panel
                  :height 300
                  :option-type :ok-cancel
                  :modal? true
                  :content (form-panel
                             :background :lightGray
                             :items
                              [[nil :fill :both :insets (java.awt.Insets. 5 5 5 5) :gridx 0 :gridy 0]
                              [(label :text "Movie" :halign :right)]
                              [(text :id :movie :columns 20) :grid :next]
                              [(label :text "Price($)" :halign :right) :gridheight 1 :grid :wrap]
                              [(text :id :price :listen [:key-pressed (fn [e] (validation e))]) :grid :next]
                              [(label :text "Quantity" :halign :right) :gridheight 2 :grid :wrap]
                              [(text :id :copies :listen [:key-pressed (fn [e] (validation e))]) :grid :next]])
                  :success-fn
                    (fn [p] (try (let [movie  (text (select (to-root p) [:#movie]))
                                       price (read-string (text (select (to-root p) [:#price])))
                                       qty (read-string (text (select (to-root p) [:#copies])))
                                       id (if(not (nil? @i)) (swap! i inc) (reset! i 1))
                                       insert->db (backend/add-movie-db movie price qty id)]
                                  {:movie movie :price price :copies qty :id id})
                             (catch Exception e (alert "Fields cannot be left blank. Try again")))))))


(defn make-edit-movie-panel
  [movie price copies id row]
   (show! (dialog :title "Edit Movies" :width 300
                  :id :make-add-movie-panel
                  :height 250
                  :option-type :ok-cancel
                  :modal? true
                  :content (form-panel
                             :background :lightGray
                             :items
                               [[nil :fill :both :insets (java.awt.Insets. 5 5 5 5) :gridx 0 :gridy 0]
                               [(label :text "Price($)" :halign :right) :gridheight 1 :grid :wrap]
                               [(text :text price :id :price :listen [:key-pressed (fn [e] (validation e))] :columns 5) :grid :next]
                               [(label :text "Quantity" :halign :right) :gridheight 2 :grid :wrap]
                               [(text :text copies :id :copies :listen [:key-pressed (fn [e] (validation e))]) :grid :next]])
                  :success-fn
                    (fn [p] (try (let [price (read-string (text (select (to-root p) [:#price])))
                                       qty (read-string (text (select (to-root p) [:#copies])))
                                       edit->db (backend/edit-movie-db movie price qty id row)]
                                   {:price price :copies qty})
                              (catch Exception e (alert "Fields cannot be left blank. Try again")))))))


(defn make-renter-panel []
  (show! (dialog :title "Rent Movies" :width 400
                 :id :make-add-movie-panel
                 :height 300
                 :option-type :ok-cancel
                 :modal? true
                 :content (form-panel
                            :background :lightGray
                            :items
                              [[nil :fill :both :insets (java.awt.Insets. 5 5 5 5) :gridx 0 :gridy 0]
                              [(label :text "Renter name" :halign :right) :gridheight 1 :grid :wrap]
                              [(text :id :renter :columns 20) :grid :next]
                              [(label :text "Due-Date" :halign :right) :gridheight 2 :grid :wrap]
                              [(label :id :date :text (str (tf/unparse my-format (-> 2 t/weeks t/from-now)))) :grid :next]])
                 :success-fn
                  (fn [p] (let [r (text (select (to-root p) [:#renter]))
                                d (text (select (to-root p) [:#date]))]
                           {:renter r :date d})))))

;;Behaviour (functionality)

(defn on-rent-table-click [e]
  (when (and (= (.getButton e) MouseEvent/BUTTON1) (= 2 (.getClickCount e)))
    (let [r-table (to-widget e)
          row (.rowAtPoint r-table (.getPoint e))]
      (when (>= row 0)
        (if (validate (return-movie-panel))
          (let [ movie (.getValueAt r-table row 0)
                 return-movie (remove-at! r-table row)
                 movie-row (first (filter #(= (get % :movie) movie) @movie-in-store))
                 qty (movie-row :copies)
                 index (.indexOf @movie-in-store movie-row)
                 remove->db (backend/remove-renter-db row)
                 movie-count inc
                 edit->db (backend/edit-movie-db index movie-count)]
            (update-at! score-table index {:copies (inc qty)})))))))

(defn on-movie-table-click
  [e]
    (when (and (= (.getButton e) MouseEvent/BUTTON1) (= 2 (.getClickCount e)))
      (let [s-table (to-widget e)
            row (.rowAtPoint s-table (.getPoint e))]
        (when (>= row 0)
          (if(not (zero? (.getValueAt s-table row 2)))
            (if-let [{:keys [renter date]} (validate (make-renter-panel))]
              (let[movie (.getValueAt s-table row 0)
                   insert-table (insert-at! make-renter-table (.getRowCount make-renter-table) {:movie movie :renter renter :date date})
                   insert->db (backend/add-renter-db movie renter date)
                   dec-qty (.setValueAt s-table (dec (.getValueAt s-table row 2)) row 2 )
                   movie-count dec
                   edit->db (backend/edit-movie-db row movie-count)]))
            (alert "Cannot rent this movie."))))))


(defn add-movie [e]
  (when-let [{:keys [movie price copies id]} (validate (make-add-movie-panel))]
    (insert-at! score-table (.getRowCount score-table) {:movie movie :price price :copies copies :id id} )))

(defn remove-movie [e]
  (if-let [row (selection score-table)]
    (if(> (.getValueAt score-table row 2) 1)
      (let [ dec-qty (.setValueAt score-table (dec (.getValueAt score-table row 2)) row 2 )
             qty? true
             remove->db (backend/remove-movie-db row qty?)])
      (if(empty? (filter #(= (get % :movie) (.getValueAt score-table row 0)) @rented-movie-in-store))
        (let[qty? false
             remove->db  (backend/remove-movie-db row qty?)
             delete-movie (remove-at! score-table row)])
        (alert "Cannot Delete. Movie is rented")))
    (alert "select row")))

(defn edit-movie [e]
  (let [row (selection score-table)
       {:keys [movie price copies id]} (value-at score-table row)]
    (if(not (nil? row))
      (if-let [{:keys [price copies]} (validate (make-edit-movie-panel movie price copies id row))]
        (update-at! score-table row {:price price :copies copies}))
    (alert "Select row"))))

(defn find-movie [value]
  (loop [row 0]
    (if (< row (.getRowCount score-table))
      (let [{:keys [movie price copies id]} (value-at score-table row)]
        (if (or (= (str id) value) (= (clojure.string/lower-case (str movie)) value))
          (alert (str "movie: " movie  " price: " price " qty: " copies))
          (recur (inc row))))
      (alert "Movie not found"))))

(defn search-movie [e]
  (let [value (text (select available-movies-tab [:#search]))
        trim-value (clojure.string/trim value)
        lowercase-value (clojure.string/lower-case trim-value)]
    (if (empty? lowercase-value)
      (alert "Input either id or name of the movie")
      (find-movie lowercase-value))))



;; Available movies and Rented movies tables

(def make-movie-table
      [:columns
        [{:key :movie :text "Movies" }
         {:key :price :text "Price($)"}
         {:key :copies :text "Quantity"}]
       :rows @movie-in-store])

(def score-table
  (doto
    (table :id :movie-table
           :model make-movie-table
           :font "ARIAL-PLAIN-14"
           :background :lightGray
           :listen [:mouse-clicked on-movie-table-click])
    (.setFillsViewportHeight true)
    (.setRowHeight 20)))


(def make-renter-table
  (table
    :background :lightGray
    :listen [:mouse-clicked on-rent-table-click]
    :model
      [:columns
       [{:key :movie :text "Movies" }
         {:key :renter :text "Renter Name"}
         {:key :date :text "Due-Date"}]
       :rows @rented-movie-in-store]))

;; UI part

(def available-movies-tab
    (mig-panel
      :constraints ["fill, ins 0"]
      :background :gray
      :items [[(action :name "Add Movie" :handler add-movie :icon (clojure.java.io/resource "add.png"))]
              [(action :name "Edit Movie" :handler edit-movie :icon (clojure.java.io/resource "edit.png"))]
              [(action :name "Remove Movie" :handler remove-movie :icon (clojure.java.io/resource "remove.png"))]
              [(horizontal-panel :items [(action :handler search-movie :icon (clojure.java.io/resource "search.jpg"))
                                         (text :id :search :columns 15)]) "wrap"]
              [(label :text "  :Double click on row to rent the movie" :font (font "ARIAL-BOLD-11")) "wrap"]
              [(scrollable score-table) "span,grow"]]))

(def rented-movies-tab
  (border-panel
    :border 5
    :north (label :text ":Double click on row to return the movie" :background :gray :font (font "ARIAL-BOLD-11"))
    :center (scrollable make-renter-table)))

(def make-toolbar
  (horizontal-panel
    :background :black
    :items [(label :icon (clojure.java.io/resource "foo.png"))
            (label :icon (clojure.java.io/resource "foo2.jpg"))
            (label :icon (clojure.java.io/resource "foo3.jpg"))
            (label :icon (clojure.java.io/resource "foo4.jpg"))
            (label :icon (clojure.java.io/resource "HDmovies.jpg"))
            (label :icon (clojure.java.io/resource "foo5.jpg"))]))

(def make-tabs
  (tabbed-panel
    :font (font "ARIAL-BOLD-14")
    :background :lightGray
    :placement :top
    :tabs [{:title "Movies Available" :content available-movies-tab}
           {:title "Movies Rented" :content rented-movies-tab}]))

(defn main-frame []
  (frame
    :title "BIG Cinemas Video store"
    :size  [800 :by 800]
    :on-close :exit
    :content (top-bottom-split
              make-toolbar
              make-tabs
              :divider-location 1/3)))

;; Main function

(defn -main [& args]
  (->(main-frame)
      show!))


























































































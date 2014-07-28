(ns mr.cpages
  (:use compojure.core
        hiccup.core
        hiccup.element
        hiccup.form
        hiccup.page
        hiccup.util)
  (:require [mr.layouts :as layout]
            [mr.models.db :as db]
            [noir.session :as sesh]
            [ring.util.codec :as codec]
            [mr.comments :as comments]))


(defn avg-grade [site-url class-url]
  (let [[{avg-grade :grade gc :gradecount}] (db/course-avg-grade site-url class-url)]
    (if (> gc 0)
      [:p "Average Grade: " (layout/grade-map (int avg-grade))]
      [:p "Average Grade: No Grades Yet."])))

(defn show-user-grade [site-url class-url]
  (if-let [suser (sesh/get :handle)]
    (let [[{student-grade :grade}] (db/get-user-course-grade site-url class-url suser)]
      (if (not (nil? student-grade))
        (if (= site-url "books")
          [:p "You gave this book a(n): " (layout/grade-map student-grade)]
          [:p "You gave this course a(n): " (layout/grade-map student-grade)])
        (if (= site-url "books")
          [:p "You have not graded this book."]
          [:p "You have not graded this course."])))))

(defn user-grade-form [site-url class-url]
  (if-let [suser (sesh/get :handle)]
    [:form {:method "POST" :action "/update-course-grade"}
     [:select {:style "width: 60px;" :required "required" :name "grade"}
      [:option ""]
      (for [ops [["A+" 12] ["A" 11] ["A-" 10] ["B+" 9] ["B" 8] ["B-" 7]
                 ["C+" 6] ["C" 5] ["C-" 4] ["D+" 3] ["D" 2] ["D-" 1] ["F" 0]]]
        [:option {:value (ops 1)} (ops 0)])]
     [:input {:type "hidden" :name "site-url" :value site-url}]
     [:input {:type "hidden" :name "class-url" :value class-url}]
     [:input {:type "hidden" :name "suser" :value suser}]
     [:input {:type "submit" :value "Grade It!" :style "margin-left: 10px;"}]]))

(defn avg-difficulty [site-url class-url]
  (let [[{avg-dif :difficulty dc :diffcount}] (db/course-avg-difficulty site-url class-url)]
    (if (> dc 0)
      [:p "Average Difficulty: " (layout/difficulty-map (int avg-dif))]
      [:p "Average Difficulty: No Ratings Yet."])))

(defn show-user-difficulty [site-url class-url]
  (if-let [suser (sesh/get :handle)]
    (let [[{student-diff :difficulty}] (db/get-user-course-difficulty site-url class-url suser)]
      (if (not (nil? student-diff))
        (if (= site-url "books")
          [:p "You gave this book a(n): " (layout/difficulty-map student-diff)]
          [:p "You gave this course a(n): " (layout/difficulty-map student-diff)])
        (if (= site-url "books")
          [:p "You have not rated this book."]
          [:p "You have not rated this course."])))))


(defn user-difficulty-form [site-url class-url]
  (if-let [suser (sesh/get :handle)]
    [:form {:method "POST" :action "/update-course-difficulty"}
     [:select {:style "width: 100px;" :required "required" :name "difficulty"}
      [:option ""]
      (for [ops [["Beginner" 1] ["Novice" 2] ["Medium" 3]
                 ["Advanced" 4] ["Only for the Brave" 5]]]
        [:option {:value (ops 1)} (ops 0)])]
     [:input {:type "hidden" :name "site-url" :value site-url}]
     [:input {:type "hidden" :name "class-url" :value class-url}]
     [:input {:type "hidden" :name "suser" :value suser}]
     [:input {:type "submit" :value "Rank It!"}]]))

(defn show-prereqs [site-url class-url]
  (let [pr (db/get-prereqs site-url class-url)]
    (if-not (empty? pr)
      (for [prereqs pr]
        [:p [:a {:href (str "/"(:prereqsite prereqs) "/" (:prerequrl prereqs))}
             (:coursename prereqs)] " by " (layout/site-url-to-site-name (:prereqsite prereqs))])
      [:p "No Prereqs Yet."])))

(defn show-prereq-form [site-url class-url coursename]
  (if-let [suser (sesh/get :handle)]
    [:form {:method "POST" :action "/create-prereqs"}
     [:input {:type "hidden" :name "site-url" :value site-url}]
     [:input {:type "hidden" :name "class-url" :value class-url}]
     [:input {:type "hidden" :name "course" :value coursename}]
     [:input {:type "submit" :value "Add a Prereq!"}]]))


(def site-image {"coursera" "/img/coursera-logo.png"
                 "codecademy" "/img/code-academy-logo.png"
                 "edx" "/img/edx-logo.png"
                 "khan-academy" "/img/kahn-academy-logo.png"
                 "udacity" "/img/udacity-logo.png"})

(defn featured-class [site-url class-url]
  [:div.row
   (let [b ((db/get-course-info site-url class-url) 0)]
     [:div.large-12.columns
      [:div.large-6.columns
       [:img {:src (site-image site-url) :alt site-url}]
       [:br]
       [:img.course-image {:src (:largeimage b) :alt (:imagealt b)}]
       [:p [:a {:href (:officialpage b)} "Official Page"]]
       [:a {:name "cg"}]
       (if (nil? (:startdate b))
         [:p "Start Date: Sign up at any time"]
         (let [sdate (layout/coerce-mdy (:startdate b))]
           [:p "Start Date: " sdate]))
       (if (not= (:timelength b) 0)
         [:p "Duration: " (:timelength b) " weeks"]
         [:p "Duration: Self-paced"])
       [:div.row
        [:div.large-7.columns
         (avg-grade site-url class-url)
         (show-user-grade site-url class-url)]
        [:div.large-5.columns
         (user-grade-form site-url class-url)]]
       [:div.row
        [:div.large-7.columns
         (avg-difficulty site-url class-url)
         (show-user-difficulty site-url class-url)]

        [:div.large-5.columns
         (user-difficulty-form site-url class-url)]]
       [:div.row
        [:div.large-12.columns
         [:h4 "Prerequisites"]
         (show-prereqs site-url class-url)
         (show-prereq-form site-url class-url (:coursename b))]]]

      [:div.large-6.columns
       [:h2.course-name (:coursename b)]
       (if (not (empty? (:prof b)))
         [:h6 "Taught By: " (:prof b)])
       [:h6 "Brief Description"]
       [:p (:description b)]
       (if (not (empty? (:bullet1 b)))
         [:h4 "Syllabus"])
       [:ul
        (for [bulls [(:bullet1 b) (:bullet2 b) (:bullet3 b) (:bullet4 b)
                     (:bullet5 b) (:bullet6 b) (:bulllet7 b) (:bullet8 b)
                     (:bullet9 b) (:bullet10 b) (:bullet11 b) (:bullet12 b)
                     (:bullet13 b) (:bullet16 b)]
              :when (not (nil? bulls))]
          [:li bulls])]]

      (let [sb (db/get-suggested-books site-url class-url)]
        (if-not (empty? sb)
          [:div.large-12.columns
           [:h3 "Suggested Books"]
           (for [suggested-books sb]
             [:div.large-3.columns
              [:div
               [:img {:src (:smallimage suggested-books) 
                      :alt (:imagealt suggested-books)}]]
              [:div
               [:span {:style "font-weight: bold;"} 
                (:bookname suggested-books)]]
              [:div
               [:a {:href (str "/books/" (:bookurl suggested-books))} 
                "See Our Review Page."]]
              [:div
               
               (:buylink suggested-books)]
              ])]))

      [:div.large-12.columns
       [:h2 "Top User Comment:"]
       (let [[{top-comment :comment}] (db/get-top-comment site-url class-url)]
         (if (not (nil? top-comment))
           [:p top-comment]
           [:p "There's no top comment yet."]))]

      (if (not (empty? (:review b)))
        [:div.large-12.columns
         [:h2 "Our Review"]
         [:p [:b "Review: "] (:review b)]
         [:p "Reviewed By: " (layout/profile-link (:reviewer b))]])

      (if-let [role (sesh/get :roles)]
        (if (= role "admin")
          [:button "Edit"]))

      [:div.large-12.columns
       [:h2 "Student Reviews and Comments"]

       (if-let [suser (sesh/get :handle)]
         (comments/comment-form suser site-url class-url)
         [:p "You must " [:a {:href "/login"} "Log In"]  
          " to leave a comment."])

       (comments/user-comments site-url class-url)

       [:pre "














"]]])])


(defn book-page [book]
  [:div.row
   (let [book-info ((db/get-book-info book) 0)]
     [:div.large-12.columns
      [:div.large-6.columns
       [:img {:src (:largeimage book-info) :alt (:imagealt book-info)}]
       [:p(:buylink book-info)]]
      [:div.large-6.columns
       [:h2 (:bookname book-info)]
       [:p (str "by: " (:author book-info))]]

      [:div.large-12.columns
       [:div.row
        [:div.large-7.columns
         (avg-grade "books" book)
         (show-user-grade "books" book)]
        [:div.large-5.columns
         (user-grade-form "books" book)]]
       [:div.row
        [:div.large-7.columns
         (avg-difficulty "books" book)
         (show-user-difficulty "books" book)]

       [:div.large-5.columns
        (user-difficulty-form "books" book)]]]
      
      (let [sc (db/get-suggested-courses book)]
        (if-not (empty? sc)
          [:div.large-12.columns
           [:h4.blue "Go-Along Courses"]
           (for [suggested-courses sc]
             [:a {:href (str "/" (:site suggested-courses) "/" 
                             (:url suggested-courses))} 
              (:coursename suggested-courses)])]))

      [:div.large-12.columns
       [:h2 "Top User Comment:"]
       (let [[{top-comment :comment}] (db/get-top-comment "books" book)]
         (if (not (nil? top-comment))
           [:p top-comment]
           [:p "There's no top comment yet."]))]

   ;; (if (not (empty? (:review b)))
   ;;   [:div.large-12.columns
   ;;    [:h2 "Our Review"]
   ;;    [:p [:b "Review: "] (:review b)]
   ;;    [:p "Reviewed By: " [:a {:href (str "/" (:reviewer b))}] (:reviewer b)]])

      (if-let [role (sesh/get :roles)]
        (if (= role "admin")
          [:button "Edit"]))
      
      [:div.large-12.columns
       [:h2 "Student Reviews and Comments"]
       
       (if-let [suser (sesh/get :handle)]
         (comments/comment-form suser "books" book)
         [:p "You must " [:a {:href "/login"} "Log In"]  " to leave a comment."])
       
       (comments/user-comments "books" book)
       
       [:pre "














"]]])])

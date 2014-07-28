(ns mr.homepage
  (:use compojure.core
        hiccup.core
        hiccup.element
        hiccup.form
        hiccup.page
        hiccup.util)
  (:require [mr.layouts :as layout]
            [mr.models.db :as db]))

(defn home-page []
  [:div.row
   [:div.large-12.columns
    (let [[{title :title content :content}] (db/get-latest-desk-article)]
      [:div.large-8.columns
       [:h4 title]
       [:article content]])
    [:div.large-4.columns
     [:h4 "Latest Reviews"]
     (for [reviews (db/select-newest-reviews)]
       [:div
        [:p [:a {:href (str (:site reviews) "/" (:url reviews))} 
             (str (:coursename reviews))]]])]]])

(defn mooc-pages [site-url]
  [:div.row
   [:div.large-12.columns
    [:div
     [:div.row
      [:div.large-3.columns
       [:h4 (layout/site-url-to-site-name site-url)]]
      [:div.large-9.columns
       [:a {:href (str site-url "/review-and-overview")} "Overview & Review"]]]
     [:h4 "Course Listings"]
     [:div.row
      [:div.large-12.columns
       (for [b (db/get-course-list site-url)]
         [:div.row.home-anchor.course-list-div.menu-anchors
          [:a {:href (str site-url "/" (:url b) )}
           [:div
            [:div.large-12.columns
             [:h5 (:coursename b)]]]
           [:div.row
            [:div.large-8.columns
             (if (not (nil? (:prof b)))
               [:p "Professor: " (:prof b)])
             (let [[{avg-grade :grade gc :gradecount}] 
                   (db/course-avg-grade site-url (:url b))]
               (if (> gc 0)
                 [:p "Average Grade: " (layout/grade-map (int avg-grade))]
                 [:p "Average Grade: No Grades Yet."]))
             (if (nil? (:startdate b))
               [:p "Start Date: Sign up at any time"]
               (let [sdate (layout/coerce-mdy (:startdate b))]
                 [:p "Start Date: " sdate]))]

            [:div.large-4.columns.course-list-image
             [:img;;.mooc-page-list-image 
              {:src (:smallimage b) :alt (:imagealt b)}]]]]])]]]]])

(defn all-courses []
  [:h2 "All Course Listings"])

(defn other-courses []
  [:h2 "Other Course Listings"])

(defn books []
  [:div.row
   [:div.large-12.columns
    [:div
     [:h4 "Book Listings"]
     (for [books (db/get-book-list)]
       [:div.row.home-anchor.course-list-div
        [:a {:href (str "books/" (:bookurl books))}
         [:div.large-8.columns
          [:h6 (:bookname books)]
          [:p (:author books)]]
         [:div.large-4.columns
          [:img {:src (:smallimage books) :alt (:imagealt books)}]]]])]]])

(defn desk-home []
  [:h5 "From the Desk"]
  (for [b (db/get-all-desk-articles)]
    [:div.row
     [:div.large-12.columns
      [:h6 (:title b)]
      [:p [:small (layout/coerce-mdy (:cdate b))]]
      (if (> (count (:content b)) 300)
        [:p (subs (:content b) 0 300)]
        [:p (:content b)])
      [:p [:a {:href (str "/desk/" (:title b))} "Read More"]]]]))

(defn desk-article-pages [article]
  (let [[{title :title cdate :cdate content :content}] 
        (db/select-desk-articles article)]
    [:div.row
     [:div.large-12.columns 
      [:h4 title]
      [:p [:small (layout/coerce-mdy cdate)]]
      [:p content]]]))

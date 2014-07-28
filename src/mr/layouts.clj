(ns mr.layouts
  (:use compojure.core
        hiccup.core
        hiccup.element
        hiccup.form
        hiccup.page
        hiccup.util)
  (:require [noir.session :as sesh]
            [clj-time.format :as ctf]
            [clj-time.coerce :as coerce]
            [mr.models.db :as db]))

(defn profile-link [profile]
  [:a {:href (str "/" (clojure.string/replace profile #" " "-"))} profile])


(def site-url-to-site-name {"coursera" "Coursera"
                            "edx" "EdX"
                            "udacity" "Udacity"
                            "khan-academy" "Khan Academy"
                            "codecademy" "Codecademy"})

(def to-mdy (ctf/formatter "MMMM d, yyyy"))

(defn coerce-mdy [sd]
  (ctf/unparse to-mdy (coerce/from-sql-date sd)))

(def grade-map {12 "A+" 11 "A" 10 "A-"
                9 "B+" 8 "B" 7 "B-"
                6 "C+" 5 "C" 4 "C-"
                3 "D+" 2 "D" 1 "D-"
                0 "F"})

(def difficulty-map {5 "Only for the Brave"
                     4 "Advanced"
                     3 "Medium"
                     2 "Novice"
                     1 "Beginner"})

(defn google-analytics []
 (javascript-tag "(function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
  (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
  m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
  })(window,document,'script','//www.google-analytics.com/analytics.js','ga');

  ga('create', 'UA-46286294-1', 'courseisland.com');
  ga('send', 'pageview');"))

(defn header [title]
  [:head
   (if (= title "Home")
     [:title "Course Island"]
     [:title title " | Course Island"])
   [:link {:rel "shortcut icon" :href "/img/island.ico" :type "image/x-icon"}]
   (include-css "/css/normalize.css")
   (include-css "/css/foundation.min.css")
   (include-css "/css/main.css")
   (google-analytics)])

(defn not-logged []
  [:div.large-5.columns
   [:a {:href "/login"} "Log In"]
   [:span "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"]
   [:a {:href "/account-creation"} "Create an Account"]])

(defn top-div []
  [:div.top-bar
   [:div.large-4.columns
    [:a {:href "/about" :style "margin-left:13em;"} "About Course Island"]]
   [:div.large-3.columns
    [:form {:method "POST" :action "/search-results"}
     [:div.row
      [:div.large-8.columns
       [:input {:type "text" :name "course-query" :class "search-box"}]]
      [:div.large-4.columns
       [:input {:type "Submit" :value "Search"}]]]]]
   (if-let [suser (sesh/get :handle)]
     [:div.large-5.columns
      [:span "Logged in as: " [:a {:href (str "/" suser)} suser]]
      (let [[{user-rep :points}] (db/get-rep suser)]
        (if (not (nil? user-rep))
          [:span " (" user-rep ")"]
          [:span " (0)"]))
      [:span "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"]
      [:a {:href "/logout"} "Logout"]]
     (not-logged))])

(defn head-div []
  [:header
   [:div.row
    [:div.large-1.columns
     [:a {:href "/"} [:img {:src "/img/island.png"}]]]
    [:div.large-11.columns
     [:div.row
      [:h2.ocw-review [:a {:href "/"} [:i {:style "color:black"} "Course Island"]]]]
     [:h6 "Is This Course Right for You?"]]]])

(def site-logo-map
  [:div.row
   [:div.large-12.columns
    [:a {:href "/coursera" :alt "Coursera"} [:img.school-logo {:src "/img/coursera-logo.png" :alt "You are viewing Coursera courses"}]]
    [:a {:href "/codecademy" :alt "Codecademy"} [:img.school-logo {:src "/img/code-academy-logo.png" :alt "Codecademy" :style "margin-right:-2em;"}]]
    [:a {:href "/edx" :alt "EdX"} [:img.school-logo {:src "/img/edx-logo.png" :alt "EdX" :style "margin-bottom:.5em;"}]]
    [:a {:href "/khan-academy" :alt "Khan Academy"} [:img.school-logo {:src "/img/kahn-academy-logo.png" :alt "Khan Academy" :style "margin-bottom:.5em;"}]]
    [:a {:href "/udacity" :alt "Udacity"} [:img.school-logo {:src "/img/udacity-logo.png" :alt "Udacity" :style "width:150px;"}]]
    [:a {:href "/books" :alt "Books" :style "margin-left:2em;"} [:img {:src "/img/book.png" :alt "Books"}]]]])


(defn page-top [title & content]
  (html5
   (header title)
   [:section
    [:div.bump-down]
    (top-div)
    (head-div)
    [:hr {:style "width:70%;margin-left:auto;margin-right:auto;"}]
    site-logo-map
    ;;      site-links
    [:hr {:style "width:70%;margin-left:auto;margin-right:auto;"}]
    (into [:div.row
           [:div.large-12.columns]] content)
    [:div.bottom-div]]))

(ns mr.about
  (:use compojure.core
        hiccup.core
        hiccup.element
        hiccup.form
        hiccup.page
        hiccup.util)
  (:require [mr.layouts :as layout]
            [mr.models.db :as db]
            [noir.session :as sesh]
            [ring.util.codec :as codec]))

(defn about-page []
  [:div.row 
   [:div.large-12.columns
    [:p "This site is written and maintained by David Toomey. If you have any questions, comments, or find any interesting bugs, please contact me at dbtoomey@gmail.com"]
    [:p "There is no FAQ yet, since that would really be a \"Questions I Wish Poeple Asked Me\" Page."]
    [:p "For now, this page will describe upcoming features. They are as follows:"]
    [:ul 
     [:li "A legitimate FAQ page"]
     [:li "Markdown capability in the comments section."]
     [:li "Search Capability."]]]])

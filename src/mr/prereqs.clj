(ns mr.prereqs
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

(defn prereq-page [site-url class-url]
  (if-let [suser (sesh/get :handle)]
    [:div.row
     [:p site-url]
     [:p class-url]
     [:div.large-12.columns
      [:h2 "Suggest Prereqs for " (str (layout/site-url-to-site-name site-url) "'s " class-url)]]
     [:form {:method "POST" :action "/add-prereqs"}
      [:hr]
      [:input {:type "hidden" :name "suser" :value suser}]
      [:input {:type "hidden" :name "site-url" :value site-url}]
      [:input {:type "hidden" :name "class-url" :value class-url}]

      ;; [:input {:type "hidden" :name "psite" :value (:site site-list)}]
      (for [site-list (db/get-sites)]
        [:div.row
         [:h4 (layout/site-url-to-site-name (:site site-list))]
         [:div.prereq-cols
          (for [prereq-list (db/get-prereq-list (:site site-list))]
            [:p [:input.prereq-ops
                 {:type "checkbox"
                  :name (str (:site site-list (:site prereq-list)) "--"
                             (:url prereq-list))
                  :value (str (:site site-list (:site prereq-list)) "--"
                              (:url prereq-list))}]
             " "(:coursename prereq-list)])]])
      [:input {:type "submit" :value "Submit"}]]]))

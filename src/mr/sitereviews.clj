(ns mr.sitereviews
  (:use compojure.core
        hiccup.core
        hiccup.element
        hiccup.form
        hiccup.page
        hiccup.util)
  (:require [mr.layouts :as layout]
            [mr.models.db :as db]
            [noir.session :as sesh]
            [noir.cookies :as cookies]
            [ring.util.codec :as codec]
            [mr.comments :as comments]))

(defn site-review [site-url]
  [:div.row
    [:div.large-12.columns
     [:h3 (layout/site-url-to-site-name site-url) " Review & Overview"]]
   [:div.row
    [:div.large-12.columns
     (let [[{review :review reviewer :reviewer}](db/get-site-reviews site-url)]
       (if (not (nil? review))
         [:div.large-12.columns
          [:p review]
          [:p  "Reviewed by: "
           (layout/profile-link reviewer)]]
         [:p "no review yet"]))
     (if-let [role (sesh/get :roles)]
       (if (= role "admin")
         (do (cookies/put! :site site-url)
             [:form {:action "/edit"}
              [:input {:type "submit" :value "Edit"}]])))]]
   [:div.large-12.columns
       [:h2 "Student Reviews and Comments"]

       (if-let [suser (sesh/get :handle)]
         (comments/comment-form suser site-url "review-and-overview")
         [:p "You must " [:a {:href "/login"} "Log In"]  " to leave a comment."])
       (comments/user-comments site-url "review-and-overview")
       [:pre "














"]]])

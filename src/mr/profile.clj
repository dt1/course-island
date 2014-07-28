(ns mr.profile
  (:use compojure.core
        hiccup.core
        hiccup.element
        hiccup.form
        hiccup.page
        hiccup.util)
  (:require [noir.session :as sesh]
            [mr.layouts :as layout]
            [mr.models.db :as db]))

(defn profile-page [profile]
  [:div.row
   [:div.row
    [:div.large-2.columns
     [:h5 profile]]
    (let [[{user-rep :points}] (db/get-rep profile)]
      [:div.large-10.columns
       [:h5 "Reputation: " user-rep]])]

   [:div.row
    [:div.large-3.columns
     [:div.profile-div-border
      (let [[{avatar :avatar}] (db/get-avatar profile)]
        [:img {:src (str "/img/avatars/" avatar)
               :alt (str profile " has no avatar")
               :style "width:200px;height200px;"}])]
     [:div.row
      (if-let [suser (sesh/get :handle)]
        (if (= suser profile)
          [:form {:method "POST"
                  :enctype "multipart/form-data"
                  :action "/update-profile-image"}
           [:p "Update your Avatar:"]
           [:input {:type "hidden" :name "suser" :value profile}]
           [:input {:type "file" :name "avatar-upload" :id "avatar-upload"}]
           [:input {:type "submit" :value "Upload" }]]))]]
    [:div.large-9.columns
     (let [[{about-user :about}] (db/get-about-user profile)]
       [:p about-user])]
    (if-let [suser (sesh/get :handle)]
      (if (= suser profile)
        [:a {:href (str "update-about-" profile)} "Update About You"]))]
   [:div.row
    [:div.large-12.columns
     [:h4 "Comments by " profile]
     (for [c (db/get-user-comments profile)]
       [:div.user-comment-list
        [:p [:b (:title c)] " on " (layout/coerce-mdy (:cdate c))]
        [:p (:comment c)]
        [:p [:a {:href (str (:site c) "/" (:url c) "#" (:cid c))} "Link"]]])]]])


(defn update-about [suser]
  (let [about-user (db/get-about-user suser)]
    [:form {:method "POST" :action "/update-about-success"}
     [:input {:type "hidden" :name "profile" :value suser}]
     [:textarea {:name "about-user" :value about-user}]
     [:input {:type "submit" :value "Update!"}]]))

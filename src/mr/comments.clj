(ns mr.comments
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

(defn comment-form [suser site-url class-url]
  [:form {:method "POST" :action "/comment"}
   [:input {:type "hidden" :name "site-url" :value site-url}]
   [:input {:type "hidden" :name "class-url" :value class-url}]
   [:input {:type "hidden" :name "suser" :value suser}]
   [:label "Leave A Comment or Review"]
   [:input {:type "text" :name "title" :required "required"}]
   [:textarea {:name "comment" :required "required"}]
   [:input {:type "submit" :value "Submit"}]])

;; (defn allow-upvote [site-url class-url suser c-info]
;;   [:div.large-1.columns
;;    [:form {:method "POST" :action "/upvote-comment"}
;;     [:input {:type "hidden" :name "site-url" :value site-url}]
;;     [:input {:type "hidden" :name "class-url" :value class-url}]
;;     [:input {:type "hidden" :name "suser" :value suser}]
;;     [:input {:type "hidden" :name "cid" :value c-info}]
;;     [:input {:type "image" :src "/img/up-arrow.png"
;;              :alt "submit" :name "submit"}]]])

;; (defn comment-reply [site-url class-url suser c-info]
;;   [:div.row
;;    [:div.large-2.columns]
;;    [:div.large-10.columns
;;     [:form {:method "POST" :action "/comment-reply"}
;;      [:input {:type "hidden" :name "site-url" :value site-url}]
;;      [:input {:type "hidden" :name "class-url" :value class-url}]
;;      [:input {:type "hidden" :name "suser" :value suser}]
;;      [:input {:type "hidden" :name "reply-to" :value c-info}]
;;      [:label "Reply to this Comment!"]
;;      [:textarea {:name "comment" :required "required"}]
;;      [:input {:type "submit" :value "Submit"}]]]])

(defn show-replies [cid]
  (for [r-info (db/get-comment-replies cid)]
    [:section {:style "margin-left: 100px;"}
     [:a {:name (:cid r-info)}]
     [:div.row
      [:div.large-12.columns
       [:p (layout/profile-link (:handle r-info))]
       (if-not (empty? (:avatar r-info))
         [:img {:src (str "/img/avatars/" (:avatar r-info)) 
                :alt (str (:handle r-info) "'s Avatar")}])
       [:p [:i (:comment r-info)]]]]]))

(defn show-comments [anc desc c-info]
  [:section
       [:div.row 
        [:a {:name (:cid c-info)}]
        [:div.large-2.columns
         [:p (layout/profile-link (:handle c-info))]
         (if-not (empty? (:avatar c-info))
           [:img {:src (str "/img/avatars/" (:avatar c-info)) 
                  :alt (str (:handle c-info) "'s Avatar")}])
         [:div.large-10.columns
          [:span [:b (:title c-info)]]
          [:p (:comment c-info)]]]]
   (show-replies (:cid c-info))])

(defn user-comments [site-url class-url]
  (for [c-info (db/get-comments site-url class-url)
        :let [anc (:ancestor c-info)
              desc (:descendant c-info)]]
    (show-comments anc desc c-info)))
    ;; [:section
    ;;  [:div.row
    ;;   [:a {:name (:cid c-info)}]
    ;;   (if-let [suser (sesh/get :handle)]
    ;;     (let [upvote-info (db/get-upvotes (:cid c-info) suser)]
    ;;       (cond (= suser (:handle c-info))
    ;;             [:div.large-1.columns]

    ;;             (not (empty? upvote-info))
    ;;             [:div.large-1.columns]

    ;;             :else
    ;;             (allow-upvote site-url class-url suser (:cid c-info))))
    ;;     [:div.large-1.columns [:img {:src "/img/up-arrow.png"}]])
    ;;   [:div.large-11.columns
    ;;    [:div.row
    ;;     [:div.large-2.columns
    ;;      [:span (layout/profile-link (:handle c-info))]
    ;;      [:br]
    ;;      [:img {:src (str "/img/avatars/" (:avatar c-info)) :alt (str (:handle c-info) "'s avatar") :style "height:75px"}] [:br]]
    ;;     [:div.large-10.columns
    ;;      [:h6 (:title c-info)]
    ;;      [:p (:comment c-info)]]]]
    ;;   (if-let [suser (sesh/get :handle)]
    ;;     (comment-reply site-url class-url suser (:cid c-info))
    ;;     [:a {:href "/login"} "Login To Leave a Reply"])]
    ;;  (show-replies (:cid c-info))]))

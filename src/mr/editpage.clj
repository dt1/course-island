(ns mr.editpage
  (:use compojure.core
        hiccup.core
        hiccup.element
        hiccup.form
        hiccup.page)
  (:require [mr.models.db :as db]
            [noir.session :as sesh]
            [ring.util.codec :as codec]))

(defn edit-page [site page]
  (html5
   (if-let [role (sesh/get :roles)]
     (if (= role "admin")
       ;; (if (= page "review-and-overview")
       (let [[{review :review}] (db/get-site-review site)]
         [:form {:method "POST" :action "/edit-article"}
          [:input {:type "hidden" :name "site" :value site}]
          [:input {:type "hidden" :name "page" :value page}]
          [:textarea {:name "review" :value review :style "width:300px;height:500px"} review]
          [:input {:type "submit" :value "Submit"}]])
       ;;   (let [[{review :review}] (db/get-course-review site site)]
       ;;     [:div
       ;;      [:form {:method "POST" :action "/edit-article"}
       ;;       [:input {:type "hidden" :name site :value site}]
       ;;       [:input {:type "hidden" :name site :value site}]
       ;;       [:textarea {:name "content" :value review}]
       ;;       [:input {:type "submit" :value "Submit"}]]])
       ))))

(ns mr.login
  (:use compojure.core
        hiccup.core
        hiccup.element
        hiccup.form
        hiccup.page
        hiccup.util)
  (:require [mr.layouts :as layout]
            [mr.models.db :as db]
            [mr.layouts :as layout]
            [noir.validation :as vali]
            [ring.util.response :as resp]
            [cemerick.friend :as friend]))

(defn valid? [username email pwd pwd2]
  (vali/min-length? username 3)
  (vali/is-email? email)
  (vali/min-length? pwd 8)
  (= pwd pwd2))

(defn create-account
  [username-unique email-unique username-length email-valid pwd-valid pwd-match]
  (layout/page-top
   "Account Creation"
     [:h3 "Create an Account"]
     [:form {:method "POST" :action "/account-creation"}
      (if (false? username-unique)
        [:p.red "Sorry, the username you chose is already taken." [:br]
             "Please choose another one."])
      (if (false? username-length)
        [:p.red "Your username must be at least 3 characters."])
      [:div "Username: (minimum 3 characters)"
       [:input.px300 {:type "text" :name "username" :required "required"}]]
      (if (false? email-unique)
        [:p.red
         "There is already an email address associated with this account."
         [:br]
         "Please " [:a {:href "/login"} "login"] " or retrieve your password"])
      (if (false? email-valid)
        [:p.red "Your email is invalid."])
      [:div "Email:"
       [:input.px300 {:type "email" :name "email" :required "required"}]]
      (if (false? pwd-valid)
        [:p.red "Your password must be at least 8 characters long."])
      (if (false? pwd-match)
        [:p.red "Your passwords do not match."])
      [:div "Password: (minumum 8 characters)"
       [:input.px300 {:type "password" :name "pwd" :required "required"}]]
      [:div "Retype your Password:"
       [:input.px300 {:type "password" :name "pwd2" :required "required"}]]
      [:div
       [:input {:type "submit" :value "Create an Account"}]]]))

(defn login-page [valid-login?]
  (layout/page-top
   "Login"
    [:h3 "Login"]
    [:form {:method "POST" :action "login"}
     (if (false? valid-login?)
       [:p.red "Either your username or password is invalid.
            Please try again or retrieve your password."])
     [:div "Username:"
      [:input.px300 {:type "text" :name "username" :required "required"}]]
    [:div "Password:"
     [:input.px300 {:type "password" :name "password" :required "required"}]]
    [:div
     [:input {:type "submit" :value "Log In"}]]]
    [:p
     [:a {:href "account-creation"} "Don't Have an Account?"]]
    [:p
     [:a {:href "retrieve-password"} "Forgot Your Password / Username?"]]))

(defn logout-success []
  (layout/page-top
   "Logout Success"
     [:br]
     [:h6 "Thank you for logging out."]))

(defn retrieve-password [valid-email?]
  (layout/page-top
   "Get Password"
   [:h3 "Enter Your Email"]
   [:form {:method "POST" :action "retrieve-password"}
    (if (false? valid-email?)
      [:p.red "Your email address is not found in our database.<br>
               Please try another email address"])
    [:div "Email"
     [:input.px300 {:type "email" :name "email" :require "required"}]]
    [:div
     [:input {:type "submit" :value "Submit"}]]]))

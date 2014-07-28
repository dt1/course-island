(ns mr.handler
  (:use compojure.core
        hiccup.core
        hiccup.page
        ring.adapter.jetty)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [mr.layouts :as layout]
            [mr.cpages :as cpage]
            [mr.coursemaps :as c-map]
            [mr.homepage :as home-page]
            [mr.models.db :as db]
            [mr.login :as log]
            [mr.sitereviews :as sitereviews]
            [mr.profile :as profile]
            [mr.prereqs :as prereqs]
            [mr.editpage :as editor]
            [mr.about :as about]
            [ring.util.response :as resp]
            [ring.util.codec :as codec]
            [noir.util.middleware :as nmw]
            [noir.util.crypt :as crypt]
            [noir.session :as sesh]
            [noir.cookies :as cookies]
            [noir.io :as io]
            [noir.validation :as vali]
            [hiccup.util :as util]
            [postal.core :as postal]
            [ring.util.codec :as codec])
  (:gen-class))

(defn set-cookie [site-url page]
  (cookies/put! :site-url site-url)
  (cookies/put! :page page))

(defn get-cookie []
  (cookies/get :site-url)
  (cookies/get :page))

(defn create-cookie-url []
  (let [site-url (cookies/get :site-url)
        page (cookies/get :page)]
    (cond (and (empty? site-url)
               (empty? page))
          "/"

          (empty? page)
          (str "/" site-url)

          :else
          (str "/" site-url "/" page))))

(defn set-user [username roles]
  (sesh/put! :handle username)
  (sesh/put! :roles roles)
  (resp/redirect (create-cookie-url)))

(defn do-login [username pwd]
  (let [[{suser :handle pwd-check :pwd roles :roles}] (db/get-users username)]
    (if (not (empty? suser))
      (if (crypt/compare pwd pwd-check)
        (set-user suser roles)
        (log/login-page false))
      (log/login-page false))))

(defn validate-new-account [username email pwd pwd2]
  (let [username-check? (empty? (db/check-username username))
        email-check? (empty? (db/check-email email))
        username-length? (vali/min-length? username 3)
        email-valid? (vali/is-email? email)
        pwd-valid? (vali/min-length? pwd 8)
        pwd-match? (= pwd pwd2)]
    (if (every? true? [username-check? email-check? username-length?
                       email-valid? pwd-valid? pwd-match?])
      (do (db/create-user username email pwd)
          (resp/redirect "/login"))
      (log/create-account username-check? email-check? username-length?
                          email-valid? pwd-valid? pwd-match?))))

(def g-chars
  (map char (concat (range 48 58)
                    (range 65 91)
                    (range 97 123))))

(defn random-characters []
  (nth g-chars (rand (count g-chars))))

(defn random-string [length]
  (apply str (take length (repeatedly random-characters))))

(defn create-temp-pwd []
  (random-string 8))

(defn update-user-password [email pwd]
  (db/update-password email pwd))

(defn validate-email [email]
  (let [email-valid? (vali/is-email? email)]
    (if (false? email-valid?)
      (resp/redirect (log/retrieve-password "This email is not in our database"))
      (let [[{snail-mail :email}] (db/get-email email)]
        (if (empty? snail-mail)
          (log/retrieve-password false))))))

(defn get-user-name [email]
  (db/get-handle email))

(defn email-temp-password [handle email pwd]
  (postal/send-message {:from "mail@courseisland.com"
                        :to email
                        :subject "Your New Temporary Password"
                        :body
                        (str "
Hello, " handle ";

You requested a new password from Course Island

Your new password is:" pwd "
You may now go to the login page at http://courseisland.com/login

Thank you so much for joining Course Island. I am always looking forward to reading your thoughts and opinions of this site. If you have any questions or concerns, please email me at my personal email: dbtooomey@gmail.com

Thank you;
David.")}))

(defn temp-login [email]
  (do (validate-email email)
      (let [temp-pwd (create-temp-pwd)
            [{handle :handle}] (get-user-name email)]
        (do (update-user-password email temp-pwd)
            (email-temp-password handle email temp-pwd)
            (resp/redirect "/")))))

(defn update-avatar [suser avatar]
  (cond  (= (:content-type avatar) "image/jpeg")
         (do
           (io/upload-file "img/avatars/"
                           (assoc avatar :filename (str suser ".jpg")))
           (db/update-avatar suser (str suser ".jpg"))
           (resp/redirect (str "/" suser)))

         (= (:content-type avatar) "image/png")
         (do (io/upload-file "img/avatars/"
                             (assoc avatar :filename (str suser ".png")))
             (db/update-avatar suser (str suser ".png"))
             (resp/redirect (str "/" suser)))

         (= (:content-type avatar) "image/gif")
         (do (io/upload-file "/img/avatars/"
                             (assoc avatar :filename (str suser ".gif")))
             (db/update-avatar suser (str suser ".gif"))
             (resp/redirect (str "/" suser)))

         :else (resp/redirect "/wrong-file-type")))

(defn sanitize-user-string [str]
  (clojure.string/replace str #"\n|<|>|&|\"|'" {"\n" "<p></p>"
                                                "<" "&#60;"
                                                ">" "&#62;"
                                                "&" "&#38;"
                                                "\"" "&#34;"
                                                "'" "&#39;"}))

(defn prereq-to-database [suser site-url class-url args]
  (doseq [[k v] args]
    (let [sc-vec (clojure.string/split v #"--")]
       (db/add-prereq site-url class-url suser (sc-vec 0) (sc-vec 1)))))

(defn course-home [site-url class-url]
  (let [[{coursename :coursename}] (db/get-course-name site-url class-url)]
    (layout/page-top
     (str (layout/site-url-to-site-name site-url) " - " coursename)
     (cpage/featured-class site-url class-url))))

(defn desk-article [article]
  (layout/page-top
   (str article)
   (home-page/desk-article-pages article)))

(defn overview-review [site-url]
  (layout/page-top
   (str site-url " Overview & Review")
   (sitereviews/site-review site-url)))

(defn all-courses-home []
  (layout/page-top
   "Code Academy Courses"
   (home-page/all-courses)))

(defn mooc-home [site-url]
  (layout/page-top
   (str (layout/site-url-to-site-name site-url) " Courses")
   (home-page/mooc-pages site-url)))

(defn books-home []
  (layout/page-top
   "Book Listings"
   (home-page/books)))

(defn book-page [book]
  (let [[{bookname :bookname}] (db/get-book-name book)]
    (layout/page-top
     (str bookname " Review")
     (cpage/book-page book))))

(defn desk-home []
  (layout/page-top
   "From the Desk"
   (home-page/desk-home)))

(defn home-page []
  (layout/page-top
   "Home"
   (home-page/home-page)))

(defroutes codecademy-routes
  (GET "/review-and-overview" []
       (do (set-cookie "codecademy" "review-and-overview")
           (overview-review "codecademy")))
  (GET "/:course-name" [course-name]
       (if (not (empty? (db/url-validate "codecademy" course-name)))
         (do (set-cookie "codecademy" course-name)
             (course-home "codecademy" course-name)))))

(defroutes coursera-routes
  (GET "/review-and-overview" []
       (do (set-cookie "coursera" "review-and-overview")
           (overview-review "coursera")))
  (GET "/:class-url" [class-url]
       (if (not (empty? (db/url-validate "coursera" class-url)))
         (do (set-cookie "coursera" class-url)
             (course-home "coursera" class-url)))))

(defroutes edx-routes
  (GET "/review-and-overview" []
       (do (set-cookie "edx" "review-and-overview")
           (overview-review "edx")))
  (GET "/:class-url" [class-url]
       (if (not (empty? (db/url-validate "edx" class-url)))
         (do (set-cookie "edx" class-url)
             (course-home "edx" class-url)))))

(defroutes khan-routes
  (GET "/review-and-overview" []
       (do (set-cookie "khan-academy" "review-and-overview")
           (overview-review "khan-academy")))
  (GET "/:class-url" [class-url]
       (if (not (empty? (db/url-validate "khan-academy" class-url)))
         (do (set-cookie "khan-academy" class-url)
             (course-home "khan-academy" class-url)))))

(defroutes udacity-routes
  (GET "/review-and-overview" []
       (do (set-cookie "udacity" "review-and-overview"))
       (overview-review "udacity"))
  (GET "/:class-url" [class-url]
       (if (not (empty? (db/url-validate "udacity" class-url)))
         (do (set-cookie "udacity" class-url)
             (course-home "udacity" class-url)))))

(defroutes book-routes
  (GET "/:book-url" [book-url]
       (if (not (empty? (db/validate-book-url book-url)))
         (do (set-cookie "books" book-url)
             (book-page book-url)))))

(defroutes desk-routes
  (GET "/:article" [article]
       (if (not (empty? (db/validate-desk-article article)))
         (do (set-cookie "desk" article)
             (desk-article article)))))

(defroutes app-routes
  (GET "/" []
       (home-page))

  (GET "/codecademy" []
       (do (set-cookie "codecademy" "")
           (mooc-home "codecademy")))
  (context "/codecademy" [] codecademy-routes)

  (GET "/coursera" []
       (do (set-cookie "coursera" "")
           (mooc-home "coursera")))
  (context "/coursera" [] coursera-routes)

  (GET "/edx" []
       (do (set-cookie "edx" "")
           (mooc-home "edx")))
  (context "/edx" [] edx-routes)

  (GET "/khan-academy" []
       (do (set-cookie "khan-academy" "")
           (mooc-home "khan-academy")))
  (context "/khan-academy" [] khan-routes)

  (GET "/udacity" []
       (do (set-cookie "udacity" "")
           (mooc-home "udacity")))
  (context "/udacity" [] udacity-routes)

  (GET "/books" []
       (do (set-cookie "books" "")
           (books-home)))
  (context "/books" [] book-routes)

  (GET "/desk" []
       (do (set-cookie "desk" "")
           (desk-home)))
  (context "/desk" [] desk-routes)

  (GET "/about" []
       (layout/page-top
        "About"
        (about/about-page)))

  (GET "/account-creation" []
       (log/create-account nil nil nil nil nil nil))

  (POST "/account-creation" {{:keys [username email pwd pwd2]} :params}
        (validate-new-account username email pwd pwd2))

  (GET "/login" []
       (log/login-page nil))

  (POST "/login" [username password]
        (do-login username password))

  (GET "/logout-success" []
       (log/logout-success))

  (ANY "/logout" []
       (do (sesh/remove! :handle)
           (resp/redirect "/")))

  (GET "/retrieve-password" []
        (log/retrieve-password nil))

  (POST "/retrieve-password" [email]
        (temp-login email))

  ;; (GET "/update-password" [])

  ;; (POST "/update-password" [pwd pwd2])

  (POST "/comment" [site-url class-url suser title comment]
        (do (db/insert-comment site-url class-url suser title
                               (sanitize-user-string comment))
            (resp/redirect (str site-url "/" class-url))))

  (POST "/update-course-grade" [site-url class-url suser grade]
        (do (db/do-course-grade site-url class-url suser (read-string grade))
            (resp/redirect (str site-url "/" class-url "#cg"))))

  (POST "/update-course-difficulty" [site-url class-url suser difficulty]
        (do (db/do-course-difficulty site-url class-url suser (read-string difficulty))
            (resp/redirect (str site-url "/" class-url "#cg"))))

  (POST "/comment-reply" [site-url class-url suser reply-to comment]
        (do (db/insert-comment-reply (read-string reply-to) site-url class-url suser
                                     (clojure.string/replace comment "\n" "<br>"))
            (resp/redirect (str site-url "/" class-url))))

  (POST "/upvote-comment" [site-url class-url cid suser]
        (do (db/do-upvote (read-string cid) suser)
            (resp/redirect (str site-url "/" class-url))))

  (POST "/create-prereqs" [site-url class-url]
        (layout/page-top
         "Prereqs"
         (prereqs/prereq-page site-url class-url)))

  (POST "/add-prereqs" [suser site-url class-url & args]
        (do
          (prereq-to-database suser site-url class-url args)
          (resp/redirect (str site-url "/" class-url))))

  (GET  "/:profile-page" [profile-page]
        (if (= (.indexOf profile-page " ") -1)
          (let [profile-page (clojure.string/replace profile-page #"-" " ")]
            (let [[{user-profile :handle}] (db/get-users profile-page)]
              (if (not (nil? user-profile))
                (layout/page-top
                 (str profile-page"'s Profile Page")
                 (profile/profile-page profile-page)))))))

  (POST "/update-profile-image" [suser avatar-upload]
        (update-avatar suser avatar-upload))

  (GET "/wrong-file-type" []
       "<p>I'm sorry, we only accept jpeg, png, and gif. Please push the back button and try again.")


  (GET "/update-about-:profile" [profile]
       (if-let [suser (sesh/get :handle)]
         (if (= suser profile)
           (layout/page-top
            "Update Profile"
            (profile/update-about suser)))))

  (POST "/update-about-success" [profile about-user]
        (db/update-about-user profile (sanitize-user-string about-user))
        (resp/redirect (str "/" profile)))

  ;; (GET "/edit-page" []
  ;;      (editor/edit-page []))

  (GET "/edit" []
       (if-let [role (sesh/get :roles)]
         (if (= role "admin")
           (let [site (cookies/get :site)
                 page (cookies/get :page)]
             (editor/edit-page site page)))))

  (POST "/edit-article" [site site-url article]
        (resp/redirect (str site "/" site-url)))

  (GET "/search-results" []
       (layout/page-top
        "No Results"
        "No Results. Please try your query again."))

  (POST "/search-results" [course-query]
       (layout/page-top
        "Search Results"
        (let [cq (db/get-search-query course-query)]
          (if-not (empty? cq)
            (for [x cq]
              [:p [:a {:href (str (:site x) "/" (:url x))} (:coursename x)]])
            "No Results. Please try your query again."))))

  (route/resources "/")
  (route/not-found "<h2>Page does not exist.</h2><img src='/img/errorGirl2.png' />"))

(def app
  (sesh/wrap-noir-session
   (cookies/wrap-noir-cookies
    (handler/site
     app-routes))))


(defn start [port]
  (run-jetty app {:port port :join? false}))

(defn -main []
  (start 3001))

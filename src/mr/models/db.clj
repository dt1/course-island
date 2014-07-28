(ns mr.models.db
  (use korma.core
       korma.db)
  (require [cemerick.friend.credentials :as creds]
           [clojure.string :as str]))

(defdb mrdb
  {:classname "org.postgresql.Driver"
   :subprotocol ""
   :subname ""
   :user ""
   :password ""})

(defentity books)
(defentity commentreplies)
(defentity comments)
(defentity commentupvotes)
(defentity coursediffs)
(defentity coursegrades)
(defentity coursenamelookup)
(defentity courses)
(defentity desk)
(defentity moocs)
(defentity prereqs)
(defentity replyupvotes)
(defentity suggestedbooks)
(defentity users)
(defentity cireviews)
(defentity commentpaths)

(defn url-validate [site-url class-url]
  (select courses
          (fields :site)
          (where {:site site-url
                  :url class-url})))

(defn get-course-info [site-url class-url]
  (select courses
          (fields :site :coursenamelookup.coursename :url :prof
                  :startdate :timelength :largeimage :imagealt :officialpage
                  :description :bullet1 :bullet2 :bullet3 :bullet4 :bullet5
                  :bullet6 :bullet7 :bullet8 :bullet9 :bullet10 :bullet11
                  :bullet12 :bullet13 :bullet14 :bullet15 :bullet16
                  :suggestedaudience :cireviews.review
                  :cireviews.reviewer)
          (join coursenamelookup (and (= :coursenamelookup.site :site)
                                      (= :coursenamelookup.url :url)))
          (join cireviews (and (= :cireviews.site :site)
                                   (= :cireviews.url :url)))
          (where {:url class-url
                  :site site-url})))

(defn get-all-courses []
  (select courses
          (fields :site
                  :coursename :url :prof :startdate :timelength :image
                  :imagealt :officialpage :description :bullet1 :bullet2
                  :bullet3 :bullet4 :bullet5 :bullet6 :bullet7 :bullet8
                  :bullet9 :bullet10 :bullet11 :bullet12 :bullet13 :bullet14
                  :bullet15 :bullet16 :suggestedaudience :cireviews.review
                  :cireviews.reviewer)
          (join coursenamelookup (and (= :coursenamelookup.site :site)
                                      (= :coursenamelookup.url :url)))

          (join cireviews (and (= :cireviews.site :site)
                                   (= :cireviews.url :url)))))

(defn get-course-list [site-url]
  (select courses
          (fields :prof :startdate :timelength :smallimage :imagealt
                  :url :coursenamelookup.coursename)
                    (join coursenamelookup 
                          (and (= :coursenamelookup.site :site)
                               (= :coursenamelookup.url :url)))

                    (where {:site site-url})
                    (order :startdate :DESC)))

(defn check-username [username]
  (select users
          (fields :handle)
          (where {(raw "LOWER(handle)") (str/lower-case username)})))

(defn check-email [email]
  (select users
          (fields :email)
          (where {(raw "LOWER(email)") (str/lower-case email)})))

(defn create-user [username email pwd]
  (insert users
          (values {:handle username
                   :email email
                   :pwd (creds/hash-bcrypt pwd)
                   :roles "use1r"
                   :rep 1})))

(defn get-users [username]
  (select users
          (fields :handle :pwd :roles)
          (where {(raw "LOWER(handle)") (str/lower-case username)})))

(defn get-comments [site-url class-url]
  (exec-raw [
"select c1.cid, c1.handle, c1.title, c1.comment, 
c1.cdate, u1.avatar 
from comments as c1
join commentpaths as cp1
on c1.cid = cp1.descendant
inner join users u1
on c1.handle = u1.handle
where not exists
	(select descendant
	from comments as c2
	join commentpaths as cp2
	on c1.cid = cp2.descendant
	and descendant <> ancestor)
and site = ?
and url = ?;" [site-url class-url]] :results))

(defn get-comment-replies [comment-id]
  (select comments
          (fields :comments.cid :commentpaths.ancestor
                  :commentpaths.descendant
                  :comments.handle :comments.title :comments.comment
                  :comments.cdate :users.avatar)
          (join commentpaths (= :comments.cid :commentpaths.descendant))
          (where (and
                  (not= :commentpaths.ancestor
                        :commentpaths.descendant)
                  (= :commentpaths.ancestor comment-id)))
          (join users (= :users.handle :comments.handle))
          (order :commentpaths.descendant :DESC)))

(defn get-comment-id [handle]
  (select comments (aggregate (max :cid) :cid)
          (where {:handle handle})))

(defn insert-comment-paths [aid did]
  (insert commentpaths
          (values {:ancestor aid
                   :descendant did})))

(defn insert-comment [site-url class-url suser title comment]
  (do (insert comments
          (values {:site site-url :url class-url :handle suser :title title
                   :comment comment}))
        (let [[{cid :cid}] (get-comment-id suser)]
          (insert-comment-paths cid cid))))

(defn insert-comment-reply [reply-to site-url class-url suser comment]
  (do (insert comments
          (values {:site site-url :url class-url :handle suser
                   :comment comment}))
      (let [[{cid :cid}] (get-comment-id suser)]
        (insert-comment-paths reply-to cid))))

(defn insert-course-grade [site-url class-url suser grade]
  (insert coursegrades
          (values {:site site-url
                   :url class-url
                   :handle suser
                   :grade grade})))

(defn update-course-grade [site-url class-url suser grade]
  (update coursegrades
          (set-fields {:grade grade})
          (where {:site site-url
                  :url class-url
                  :handle suser})))

(defn check-course-grades [site-url class-url suser]
  (select coursegrades
          (fields :handle)
          (where {:site site-url
                  :url class-url
                  :handle suser})))

(defn get-user-course-grade [site-url class-url suser]
  (select coursegrades
          (fields :grade)
          (where {:site site-url
                  :url class-url
                  :handle suser})))


(defn insert-course-difficulty [site-url class-url suser grade]
  (insert coursediffs
          (values {:site site-url :url class-url :handle suser :difficulty grade})))

(defn update-course-difficulty [site-url class-url suser grade]
  (update coursediffs
          (set-fields {:difficulty grade})
          (where {:site site-url
                  :url class-url
                  :handle suser})))

(defn check-course-difficulty [site-url class-url suser]
  (select coursediffs
          (fields :handle)
          (where {:site site-url
                  :url class-url
                  :handle suser})))

(defn do-course-difficulty [site-url class-url suser difficulty]
  (if (empty? (check-course-difficulty site-url class-url suser))
    (insert-course-difficulty site-url class-url suser difficulty)
    (update-course-difficulty site-url class-url suser difficulty)))

(defn get-user-course-difficulty [site-url class-url suser]
  (select coursediffs
          (fields :difficulty)
          (where {:site site-url
                  :url class-url
                  :handle suser})))


(defn course-avg-grade [site-url class-url]
  (select coursegrades
          (aggregate (avg :grade) :grade)
          (aggregate (count :grade) :gradecount)
          (where {:site site-url
                  :url class-url})))

(defn course-avg-difficulty [site-url class-url]
  (select coursediffs
          (aggregate (avg :difficulty) :difficulty)
          (aggregate (count :difficulty) :diffcount)
          (where {:site site-url
                  :url class-url})))

(defn do-course-grade [site-url class-url suser grade]
  (if (empty? (check-course-grades site-url class-url suser))
    (insert-course-grade site-url class-url suser grade)
    (update-course-grade site-url class-url suser grade)))


(defn do-upvote [cid handle]
  (insert commentupvotes
          (values {:cid cid :handle handle})))

(defn get-upvotes [cid handle]
  (select commentupvotes
          (fields :cid
                  :handle)
          (where {:cid cid
                  :handle handle})))

(defn get-sites []
  (select courses
          (fields :site)
          (modifier "distinct")))

(defn get-prereq-list [site-url]
  (select courses
          (fields :site :url :coursenamelookup.coursename)
          (join coursenamelookup (and (= :coursenamelookup.site :site)
                                      (= :coursenamelookup.url :url)))
          (where {:site site-url})
          (order :coursenamelookup.coursename)))

(defn get-prereqs [site-url class-url]
  (select prereqs
          (fields :prereqsite :prerequrl :coursenamelookup.coursename)
          (join coursenamelookup
                (and (= :coursenamelookup.site :prereqsite)
                     (= :coursenamelookup.url :prerequrl)))
          (where {:site site-url
                  :url class-url})))

(defn add-prereq [site url suser psite purl]
  (insert prereqs
          (values {:site site
                   :url url
                   :handle suser
                   :prereqsite psite
                   :prerequrl purl})))

(defn get-site-reviews [site-url]
  (select cireviews
          (fields :review :reviewer)
          (where {:site site-url
                  :url "review-and-overview"})))


(defn get-rep [suser]
  (select commentupvotes
          (aggregate (sum :point) :points)
          (where {:handle suser})))

(defn get-user-comments [user]
  (select comments
          (fields :cid :site :url :handle :title :comment :cdate)
          (where {:handle user})))

(defn insert-about-user [suser about]
  (insert users
          (values {:about about})
          (where {:handle suser})))

(defn update-about-user [suser about]
  (update users
          (set-fields {:about about})
          (where {:handle suser})))

(defn check-about-user [suser]
  (select users
          (fields :about)
          (where {:handle suser})))

(defn do-about-user [suser about]
  (if (empty? (check-about-user suser))
    (insert-about-user suser about)
    (update-about-user suser about)))

(defn get-about-user [user]
  (select users
          (fields :about)
          (where {:handle user})))

(defn get-top-comment [site-url class-url]
  (exec-raw ["select 	cu.cid, comments.site, comments.url,
	comments.handle, comments.title, comments.comment,
	comments.cdate, max(cu.point) as maxpoint
from 	comments
	natural join
	(select cid, sum(point) as point
	from commentupvotes
	group by cid) as cu
where comments.cid = cu.cid
and comments.site = ?
and comments.url = ?
group by cu.cid, comments.site, comments.url,
	comments.handle, comments.title, comments.comment,
	comments.cdate, comments.cid
order by maxpoint desc
limit 1;" [site-url class-url]] :results))

(defn validate-book-url [book]
  (select books
          (fields :bookurl)
          (where {:bookurl book})))

(defn get-book-list []
  (select books
          (fields :bookurl :bookname :author :smallimage :imagealt)))

(defn get-book-info [book]
  (select books
          (fields :bookname :author :largeimage :imagealt :buylink)
          (where {:bookurl book})))

(defn get-book-name [book-url]
  (select books
          (fields :bookname)
          (where {:bookurl book-url})))

(defn get-site-review [site-url]
  (select cireviews
          (fields :review)
          (where {:site site-url
                  :url "review-and-overview"})))

(defn validate-desk-article [article]
  (select desk
          (where {:title article})))

(defn select-desk-articles [article]
  (select desk
          (fields :title :content :cdate)
          (where {:title article})
          (order :cdate)
          (limit 1)))

(defn get-all-desk-articles []
  (select desk
          (fields :title :content :cdate)
          (order :cdate)))

(defn get-latest-desk-article []
  (select desk
          (fields :title :content)
          (order :cdate)
          (limit 1)))

(defn select-newest-reviews []
  (select cireviews
          (fields :cireviews.site :cireviews.url
                  :cireviews.reviewer :coursenamelookup.coursename)
          (join coursenamelookup (and (= :coursenamelookup.site :site)
                                      (= :coursenamelookup.url :url)))
          (order :rdate)
          (limit 5)))

(defn update-avatar [suser avatar]
  (update users
          (set-fields {:avatar avatar})
          (where {:handle suser})))

(defn get-avatar [suser]
  (select users
          (fields :avatar)
          (where {:handle suser})))


(defn get-suggested-books [site-url class-url]
  (select suggestedbooks
          (fields :suggestedbooks.bookurl
                  :books.bookname
                  :books.author
                  :books.smallimage
                  :books.imagealt
                  :books.buylink)
          (join books (= :books.bookurl :suggestedbooks.bookurl))
          (where {:site site-url
                  :url class-url})))

(defn get-suggested-courses [book-url]
  (select suggestedbooks
          (fields :coursenamelookup.site
                  :coursenamelookup.url
                  :coursenamelookup.coursename)
          (join coursenamelookup 
                (and (= :suggestedbooks.url :coursenamelookup.url)
                     (= :suggestedbooks.site :coursenamelookup.site)))
          (where {:suggestedbooks.bookurl book-url})))

(defn get-email [email]
  (select users
          (fields :email)
          (where {:email email})))

(defn update-password [email pwd]
  (update users
          (set-fields {:pwd (creds/hash-bcrypt pwd)})
          (where {:email email})))

(defn get-handle [email]
  (select users
          (fields :handle)
          (where {:email email})))

(defn update-article [site url]
  )

(defn get-course-name [site-url class-url]
  (select coursenamelookup
          (fields :coursename)
          (where {:site site-url
                  :url class-url})))

(defn get-search-query [course-query]
  (exec-raw [
"select cs.site, cs.url, cnl.coursename
from courses cs
inner join coursenamelookup cnl
on cs.site = cnl.site
and cs.url = cnl.url
where to_tsquery(?) @@ to_tsvector(coursename);" [course-query]] :results))

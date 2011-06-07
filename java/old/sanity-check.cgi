#!/home/cug/mcj4/bin/plt/bin/mzscheme -gqr 

;;On my Powerbook, Apache2 won't find the collections
;; for my installation of PLT Scheme. Unfortunately,
;; that makes both the application path and the path
;; to the libraries something that must be changed on each
;; new platform.
(current-library-collection-paths 
 (list (build-path "/home/cug/mcj4/bin/plt/collects")
       (build-path "/home/cug/mcj4/.plt-scheme/205/collects")
       ))

(require (lib "cgi.ss" "net")
	 (lib "db.ss" "util"))

;;CONSTANTS
(define *SUCCESS* 4)
(define *WARNING* 3)
(define *ERROR*   2)
(define *FAILURE* 1)

(define metas-table-name
  "f2003metas031009")

(define errors-table-name
  "f2003errors031009")

;;Turn on or off debugging in the script
(define *DEBUG* #t)
(current-error-port 
 (open-output-file "/home/cug/mcj4/public_html/f2003/error_log" 'append))


;;Send some headers to keep the client
;; happy.
(output-http-headers)

(set-host!     "penguin.kent.ac.uk")
(set-database! "mcj4")
(set-user!     "mcj4")
(set-password! "oohoo3j")

(get-connection)

;;DATASTRUCTURES
;; These structures hold the data as it is pulled from
;; the POST. It makes it easy to extract the data
;; from the records when it comes time to bundle it all up
;; into an SQL query.
(define-struct metas
  (index client-index uname homedir result client-start client-duration
	 server-receive ip-address os-name os-arch os-version))

(define-struct error
  (meta uname etype etime emsg eline fname file))

#|tables

create table f2003metas031009 (
index serial,
client_index integer,
uname text,
homedir text,
result integer,
client_start integer,
client_duration integer,
server_receive integer,
ip_address text,
os_name text,
os_arch text,
os_version text);

create table f2003errors031009 (
index serial,
meta integer,
uname text,
etype integer,
etime integer,
emsg text, 
eline integer,
fname text,
file text);
|#


;;CONTRACT
;; go :: nothing -> void
;;PURPOSE
;; This is the whole show, right here. Everything. 
(define (go)
  (let* ([bindings (get-bindings)]
	 [next-meta (get-next-meta-index)])
    
    ;;Create datastructures containing info to ship 
    ;; to the DB that have gone through some kind of 
    ;; sanity checks, just in case we get weird
    ;; garbage from the BlueJ extension.
    (let ([o  (make-checked-metas bindings next-meta)]
	  [e* (make-checked-errors bindings next-meta)])
      ;;Ship the checked data off to the DB.
      (load-metas o)
      (load-errors e*))    
    ))


;;CONTRACT
;; load-errors :: list-of-errors -> void
;;PURPOSE
;; Takes a list of error-structs, and processes
;; each one in turn, firing off an SQL INSERT
;; to the DB.  
(define load-errors
  (lambda (e*)
    (foreach ([e e*])
      (let ([the-query
	     (sql-format
	      (string-append "INSERT INTO " errors-table-name)
	      ;;  (meta uname etype etime emsg eline fname file))
	      "(meta,uname,etype,etime,emsg,eline,fname,file)"
	      "VALUES (~a,~a,~a,~a,~a,~a,~a,~a)"
	      `(int     ,(error-meta e))
	      `(text    ,(error-uname e))
	      `(int     ,(error-etype e))
	      `(int     ,(error-etime e))
	      `(text    ,(error-emsg e))
	      `(int     ,(error-eline e))
	      `(text    ,(error-fname e))
	      `(text    ,(error-file e)))])
	;;(debug 
	;; (lambda ()
	;; (fprintf (current-error-port) "~n~a~n" the-query)))
	(send (current-connection) query the-query))
      )))


;;CONTRACT
;; load-metas :: metas -> void
;;PURPOSE
;; Takes an metas-struct, and blasts off the contents
;; as an SQL query to the DB.
(define load-metas
  (lambda (o)
    (let ([the-query
	   (sql-format
	    (string-append "INSERT INTO " metas-table-name)
	    (string-append
	     "(index,client_index,uname,homedir,result,client_start,"
	     "client_duration,"
	     "server_receive,ip_address,os_name,os_arch,os_version)")
	    "VALUES (~a,~a,~a,~a,~a,~a,~a,~a,~a,~a,~a,~a)"
	    `(int     ,(metas-index o))
            `(int     ,(metas-client-index o))
	    `(text    ,(metas-uname o))
	    `(text    ,(metas-homedir o))
	    `(int     ,(metas-result o))
	    `(int     ,(metas-client-start o))
	    `(int     ,(metas-client-duration o))
	    `(int     ,(metas-server-receive o))
	    `(text    ,(metas-ip-address o))
	    `(text    ,(metas-os-name o))
	    `(text    ,(metas-os-arch o))
	    `(text    ,(metas-os-version o)))])
      (debug 
       (lambda ()
       (fprintf (current-error-port) "~n~a~n" the-query)))
      (send (current-connection) query the-query))))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; BUILDERS
;; These procedures help build the data structures that get 
;; processed for shipping. In particular, they make sure that 
;; the values being sent to the DB are sane.

(define make-checked-metas
  (lambda (bindings next-meta)
    (let ([get (lambda (s) 
		 (extract-binding/single s bindings))])
      (make-metas
       next-meta
       (get "CLIENTINDEX")
       (get "SYSUSER")
       (get "HOME")
       (result bindings)
       (mili->sec (get "CSTART"))
       (get "CDUR")
       (current-seconds)
       (get "IPADDR")
       (get "OSNAME")
       (get "OSARCH")
       (get "OSVER")))))

;;This is currently completely untested.
;; but as of rev 031009, it seems to work. 
(define get-next-meta-index
  (lambda ()
    (let ([the-query
	   (string-append
	    "SELECT nextval('"
	    metas-table-name
	    "_index_seq');")])
      ;;(debug 
      ;; (lambda ()
      ;; (fprintf (current-error-port) "~n~a~n" the-query)))
      (let* ([result
	      (send (current-connection) query the-query)]
	     [res (vector-ref (car (recordset-rows result)) 0)])
	(debug (lambda () res))
	res))))
    
;;  (meta uname etype etime emsg eline fname file))
(define make-checked-error
  (lambda (type bindings next-meta)
    (let ([get (lambda (s) 
		 (extract-binding/single s bindings))])
      (cond
       [(member type '("SUCCESS" "FAILURE"))
	(make-error 
	 next-meta
	 (get "SYSUSER")
	 (if (equal? "SUCCESS" type) *SUCCESS* *FAILURE*)
	 (mili->sec (get (string-append type "_TIME")))
	 ""
	 -1
	 (get (string-append type "_FILENAME"))
	 (clean-string (get (string-append type "_FILE")))
	 )]
       [(member type '("ERROR" "WARN"))
	(make-error
	 next-meta
	 (get "SYSUSER")
	 (if (equal? "ERROR" type) *ERROR* *WARNING*)
	 (mili->sec (get (string-append type "_TIME")))
	 (clean-string (get (string-append type "_MSG")))
	 (get (string-append type "_LINE"))
	 (get (string-append type "_FILENAME"))
	 (clean-string (get (string-append type "_FILE")))
	  )]
       ))))
	 

(define make-checked-errors
  (lambda (bindings next-meta)
    (let ([gets (lambda (s)
		  (extract-bindings s bindings))])
      (let ([error-types (gets "EVENT_TYPE")])
	(map (lambda (t)
	       (make-checked-error t bindings next-meta))
	     error-types)))))
	


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; UTIL
;; These are helper procedures used by the heavy-lifting
;; procedures of the CGI.


;;CONTRACT
;; debug :: thunk -> void
;;PURPOSE
;; Takes a debugging thunk, and conditionally
;; executes it based on a global variable.
(define debug 
  (lambda (thunk)
    (if *DEBUG* (thunk))))


;; CONTRACT 
;; mili->sec :: string -> number
;; PURPOSE
;; Takes a Java timestamp (miliseconds since Jan 1, 1970) and
;; trims it down to the number of seconds since Jan 1.
(define mili->sec
  (lambda (n)
    (let ([n (string->number n)])
      (inexact->exact (round (/ n 1000)))
      )))

;;CONTRACT
;; result :: bindings -> string
;;PURPOSE
;; Takes the POST bindings, and checks to see 
;; if it was a successful (SUCCESS) or unsuccessful (FAILURE) compile.
;; If it is successful, return 1.
;; If it is a failure, return 0.
(define result
  (lambda (bindings)
    (if (member "SUCCESS" (extract-bindings "EVENT_TYPE" bindings))
	1
	0)))
	


;;CONTRACT
;; clean-string :: string -> string
;;PURPOSE
;; Used to take a string and base64 encode it,
;; also replacing return/newline pairs with a hyphen.
;; Now, it does nothing.
(define clean-string
  (lambda (s)
    ;;(pregexp-replace* "\r\n" (base64-encode s) "-")))
    s))
    

	      
 
;; This is just a debugging loop; it
;; loops over all the bindings received,
;; and sends them to STDERR. Under Apache,
;; this gets caught in the error log.
; (foreach ([binding (get-bindings)])
;   (fprintf (current-error-port) 
; 	   "~a <-- ~a~n" 
; 	   (car binding) 
; 	   (cdr binding)))

;;Run the bugger.
(go)

;;And we're outta here... (Dennis Miller)
(newline)

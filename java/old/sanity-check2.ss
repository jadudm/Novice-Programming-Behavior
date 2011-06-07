#!/usr/bin/mzscheme -gqr 


;;On my Powerbook, Apache2 won't find the collections
;; for my installation of PLT Scheme. Unfortunately,
;; that makes both the application path and the path
;; to the libraries something that must be changed on each
;; new platform.
(current-library-collection-paths 
 `(,@(current-library-collection-paths)
   ;;,(build-path "~/.plt-scheme/205/collects")
   ,(build-path "/Volumes/big/Users/mcj4/Library/PLT Scheme/205/collects")
   ))


(require (lib "cgi.ss" "net")
	 (lib "base64.ss" "net")
	 (lib "msql.ss" "msql"))

;;CONSTANTS
(define *SUCCESS* 4)
(define *WARNING* 3)
(define *ERROR*   2)
(define *FAILURE* 1)

(define metas-table-name "sp2004metas040105")
(define errors-table-name "sp2004errors040105")

;;Turn on or off debugging in the script
(define *DEBUG* #t)

(current-error-port 
 (open-output-file "error_log2" 'append))

;;Send some headers to keep the client
;; happy.
(output-http-headers)

;;(set-host!     "penguin.kent.ac.uk")
;;(set-database! "mcj4")
;;(set-user!     "mcj4")
;;(set-password! "oohoo3j")

(define <C> (connect-to-database "localhost" 5432 "mcj4" "mcj4" "vegowelt"))

;;DATASTRUCTURES
;; These structures hold the data as it is pulled from
;; the POST. It makes it easy to extract the data
;; from the records when it comes time to bundle it all up
;; into an SQL query.
(define-struct metas
  (index client-index uname location homedir filename
	 file_index result client_start client_duration
	 server_receive ip_address hostname os_name os_arch os_version))

(define-struct error
  (meta uname etype etime emsg eline fname file))


;;CONTRACT
;; go :: nothing -> void
;;PURPOSE
;; This is the whole show, right here. Everything. 
(define (go)
  (let* ([bindings (get-bindings)]
	 [next-meta (get-next-meta-index)])

    (debug (lambda () (fprintf (current-error-port)
			       "Bindings : ~a" bindings)))
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
    (for-each 
     (lambda (e)
       (let ([the-query
	      (insert ,errors-table-name
		      (meta uname etype etime emsg eline fname file)
		      ,(list 
			(error-meta e)
			(error-uname e)
			(error-etype e)
			(error-etime e)
			(error-emsg e)
			(error-eline e)
			(error-fname e)
			(error-file e)))])
	 (schemeql-execute the-query <C>)))
     e*)))



;;CONTRACT
;; load-metas :: metas -> void
;;PURPOSE
;; Takes an metas-struct, and blasts off the contents
;; as an SQL query to the DB.

(define load-metas
  (lambda (o)
    (let ([the-query
	   (insert ,metas-table-name
		   (index client_index uname location homedir filename
			  file_index result client_start client_duration
			  server_receive ip_address hostname os_name os_arch
			  os_version)
		   ,(list 
		     (metas-index o)
		     (metas-client-index o)
		     (metas-uname o)
		     (metas-location o)
		     (metas-homedir o)
		     (metas-filename o)
		     (metas-file_index o)
		     (metas-result o)
		     (metas-client_start o)
		     (metas-client_duration o)
		     (metas-server_receive o)
		     (metas-ip_address o)
		     (metas-hostname o)
		     (metas-os_name o)
		     (metas-os_arch o)
		     (metas-os_version o)))])
      (schemeql-execute the-query <C>)
      )))


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
       (get "LOCATION")
       (get "HOME")
       (get "THISFILE_FILENAME")
       (get "THISFILE_COUNT")
       (get-result bindings)
       (mili->sec (get "CSTART"))
       (get "CDUR")
       (current-seconds)
       (get "IPADDR")
       (get "HOSTNAME")
       (get "OSNAME")
       (get "OSARCH")
       (get "OSVER")))))

;;This is currently completely untested.
;; but as of rev 031009, it seems to work. 
(define get-next-meta-index
  (lambda ()
    (let ([the-query
	   (query 
	    ,(string-append " nextval('" metas-table-name "_index_seq');"))])
      ;;(debug 
      ;; (lambda ()
      ;; (fprintf (current-error-port) "~n~a~n" the-query)))
      (let* ([result
	      (cursor-car (result-cursor (schemeql-execute the-query <C>)))]
	     [res (car result)])
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
		  (map (lambda (s)
			 (base64-decode s))
		       (extract-bindings s bindings)))])
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
    (if *DEBUG* (thunk) (flush-outport (current-error-port)))))


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
(define get-result
  (lambda (bindings)
    (if (member "SUCCESS" 
		(map (lambda (s)
		       (base64-decode s))
		     (extract-bindings "EVENT_TYPE" bindings)))
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




#|tables

create table metas (
index serial,
client_index integer,
uname text,
location text, ;;new
homedir text, ;;new
filename text, ;;new
file_index integer, ;;new
result integer,
client_start integer,
client_duration integer,
server_receive integer,
ip_address text,
hostname text, ;;new
os_name text,
os_arch text,
os_version text);

(require (lib "msql.ss" "msql"))
(begin 
  (define <c> (connect-to-database "penguin.kent.ac.uk" 5432 "mcj4" "mcj4" "oohoo3j"))
  (define metas-table-name "sp2004metas040105")
  (define errors-table-name "sp2004errors040105")
  (define <t>
    (create-table 
     ,metas-table-name
     (index serial)
     (uname text)
     (location text)
     (client_index integer)
     (file_index integer)
     (result integer)
     (filename text)
     (homedir text)
     (client_start integer)
     (client_duration integer)
     (server_receive integer)
     (ip_address text)
     (hostname text)
     (os_name text)
     (os_arch text)
     (os_version text)))
  (schemeql-execute <t>))

create table errors (
index serial,
meta integer,
uname text,
etype integer,
etime integer,
emsg text, 
eline integer,
fname text,
file text);

(require (lib "msql.ss" "msql"))
(begin 
  (define <c> (connect-to-database "penguin.kent.ac.uk" 5432 "mcj4" "mcj4" "oohoo3j"))
  (define metas-table-name "sp2004metas040105")
  (define errors-table-name "sp2004errors040105")
  (define <t>
    (create-table 
     ,errors-table-name
     (index serial)
     (meta integer)
     (uname text)
     (etype integer)
     (etime integer)
     (emsg text)
     (eline integer)
     (fname text)
     (file text)))
  (schemeql-execute <t>))
|#

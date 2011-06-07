#lang scheme/gui
(require (planet schematics/xmlrpc:4:0/xmlrpc))
(require net/url)
 
; Create a dialog
(define frame (instantiate frame% ("NCB Server Control")))

; Add a text field to the dialog
(define vp (new vertical-panel% [parent frame]))

(define p2 (new horizontal-panel% [parent vp]))
(define m2 (new message% [parent p2] [label "Server"] [min-width 100]))
(define server-field
  (new text-field% [parent p2] [label ""] [style '(single)] [min-width 200]))

(define p3 (new horizontal-panel% [parent vp]))
(define m3 (new message% [parent p3] [label "Port"] [min-width 100]))
(define port-field
  (new text-field% [parent p3] [label ""] [style '(single)] [min-width 100]))

(define p1 (new horizontal-panel% [parent vp]))
(define m1 (new message% [parent p1] [label "Password"] [min-width 100]))
(define password-field
  (new text-field% [parent p1] [label ""] [style '(single password)] [min-width 200]))

;; A message pane
(define p4 (new horizontal-panel% [parent vp]))
(define m4 (new message% [parent p4] [label ""] [min-width 300]))

; Add a horizontal panel to the dialog, with centering for buttons
(define bottom-panel (new horizontal-panel% [parent vp]
                          [alignment '(center center)]))


(define (quiet-call thunk)
  (with-handlers ([exn? 
                   (lambda (e) 'fail)])
    (thunk)))
                     
(define toggle
  (let ([state #t])
    (lambda (b e)
      (let* ([server (xmlrpc-server (send server-field get-value) 
                                    (send port-field get-value)
                                    "RPC2")]
             [setState (server "setState")]
             [result (quiet-call
                      (lambda ()
                        (setState (send password-field get-value) (not state))))])
        ;; Set the state to the returned result... since that is accurate.
        (set! state result)
        
        (if state
            (begin (send b set-label "Stop"))
            (begin (send b set-label "Start"))
        )))))

    
; Add Cancel and Ok buttons to the horizontal panel
(define b
  (new button% 
       [parent bottom-panel]
       [label "Stop"]
       [callback toggle]
       ))

; Show the dialog
(send frame show #t)

(define reference (current-seconds))
(thread (lambda ()
          (let loop ([count 0])
            (sleep 10)
            
            (let* ([server (xmlrpc-server (send server-field get-value) 
                                          (send port-field get-value)
                                          "RPC2")]
                   [getState (server "getState")]
                   [result
                    (quiet-call
                     (lambda () (getState)))])
              
              (if (boolean? result)
                  (send m4 set-label 
                        (format "[~a] Server up." count))
                  (send m4 set-label 
                        (format "[~a] Server down." count))))
            (loop (add1 count))
            )))
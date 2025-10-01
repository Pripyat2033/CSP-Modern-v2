(asdf:defsystem #:aecs2
  :description "The LISP-based core for the Automated Error Correction System, Version II."
  :author "AECS-II Team"
  :license "Proprietary"
  :version "0.0.1"
  :serial t
  :depends-on (#:cl-json #:cl-base64)
  :components ((:file "aecs-core")))
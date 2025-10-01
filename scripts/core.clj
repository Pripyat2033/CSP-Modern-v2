(ns aecs2.core
  (:require [cheshire.core :as json]
            [clojure.java.io :as io])
  (:gen-class))

(defn -main
  "The main entry point for the AECS-II Clojure engine.
  It reads a JSON request from standard input, parses it, and begins the
  error correction process."
  [& args]
  (println "AECS-II Clojure Core Initialized. Awaiting instructions on stdin...")

  (if-let [line (read-line)]
    (let [request (json/parse-string line true)
          file-path (:filePath request) ; The path to the file with an error
          error-msg (:errorMessage request) ; The specific error message
          ast (:ast request)] ; The full AST of the file, parsed as a Clojure map
      (println "Received and parsed AST for file:" file-path)

      ;; This is where the "Deductive Synthesizer" logic will begin.
      ;; For now, we just prove we can access the AST.
      (let [package-name (get-in ast [:packageDeclaration :name :identifier])]
        (println "Identified package from AST:" package-name)))
    (println "No input received on stdin. Exiting.")))
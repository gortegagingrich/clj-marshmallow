(ns marshmallow.core
  (:gen-class)
  (:require
    [marshmallow.extract-script :as ext]
    [marshmallow.insert-script :as ins]))

(defn -main
  "isolates messages in all of the files in scripts/ and writes them to formatted files to be edited in translation"
  ([action]
    (case action
      "extract" (ext/extract
                  (rest
                    (file-seq
                      (clojure.java.io/file "scripts/"))))
      "insert" (ins/batch-insert
                 (rest
                   (file-seq
                     (clojure.java.io/file "tl/"))))
      "default" (println "Invalid argument.\nOnly takes (extract or insert)"))))

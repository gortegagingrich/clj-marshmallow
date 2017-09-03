(ns marshmallow.core
  (:gen-class)
  (:require
    [marshmallow.parse-script :refer :all]
    [marshmallow.insert-script :as insert]))

(defn extract
  [files]
  (doseq [file files]
    (try
      (let [msg (get-messages file)]
        (with-open
          [w (clojure.java.io/writer (str "tl/" (.getName file) ".txt"))]
          (doseq
            [line
             (cons
               (format
                 "// line count: %d\n// char count: %d\n\n// speakers\n%s\n// text\n"
                   (count msg)
                   (count-chars file)
                   (unique-speakers msg))
                 (get-lines msg))]
              (.write w line))))
        (catch Exception e
          (println
            (format
              "Could not read file \"%s\": %s"
              (.getName file)
              (.toString e)))))))

(defn insert
  []
  (insert/batch-insert))

(defn -main
  "isolates messages in all of the files in scripts/ and writes them to formatted files to be edited in translation"
  ([action]
    (case action
      "extract" (extract
                  (rest (file-seq (clojure.java.io/file "scripts/"))))
      "insert" (insert)
      "default" (println "Invalid argument.\nOnly takes (extract or insert)"))))

(ns marshmallow.insert-script
  (:require
    [instaparse.core :as insta]
    [clojure.xml :as xml]))

(def tl-file
  (insta/parser
    "<S> = NameSection LineSection
    Comment = '//' #'[^\n]*\n'
    NameSection = Names
    LineSection = Lines
    <Names> = <Comment> | NamePair | Names Names
    NamePair = JpnName EngName
    JpnName = <'<jpn>'> Text
    EngName = <'<eng>'> Text
    <Lines> = <Comment> | LinePair | Lines Lines
    LinePair = <'<jpn '> ID <'>'> JpnLine EngLine
    JpnLine = Text
    EngLine = <'<eng ' ID '>'> Text
    ID = #'[0-9]+'
    <Text> = #'[^\n]*'"
    :auto-whitespace :standard))

(defn get-match
  [tree n]
  (->>
    tree
    (filter
      (fn [x]
        (=
          n
          (get-in x [1 1]))))
    (first)
    (rest)
    (vec)))

(defn get-from-tree
  [tree n i]
  (let
    [s (let
         [match (get-match tree n)
          res (get-in match [(inc i) 1])]
         (if
           (empty? res)
           (get-in match [i 1])
           res))]
    (if
      (empty? s)
      ""
      s)))

(defn get-line
  [tree n]
  (clojure.string/replace
    (get-from-tree
      (second tree)
      n
      1)
    "\""
    "&quot;"))

(defn get-name
  [tree n]
  (get-from-tree
    (first tree)
    n
    0))

(defn print-attr
  [attr w]
  (try
    (.write
      w
      (format
        " %s=\"%s\""
        (subs
          (clojure.string/replace
            (str
              (first attr))
            "_at_l"
            "@l")
          1)
        (str
          (second attr))))
    (catch Exception e)))


(defn print-xml
  [element tree w]
  (if
    (not-empty
      (get element :content))
    (do
      (.write
        w
        (format
          "<%s>\n"
          (subs
            (str
              (get element :tag))
            1)))
      (print-xml
        (doseq
          [child (get element :content)]
          (print-xml child tree w))
        tree
        w)
      (.write
        w
        (format
          "</%s>\n"
          (subs
            (str
              (get element :tag))
            1))))
    (let
      [len (count
             (str
               (get element :tag)))]
      (when
        (> len 0)
        (.write
          w
          (format
            "\t\t<%s"
            (subs
              (str
                (get element :tag))
              1)))
        (doseq
          [attr (get element :attrs)]
          (case
            (first attr)
            :name (.write
                    w
                    (format
                      " name=\"%s\""
                      (let
                        [n (get-name
                             tree
                             (second attr))]
                        (if
                          (empty? n)
                          ""
                          n))))
            :text (.write
                    w
                    (format
                      " text=\"%s\""
                      (get-line
                        tree
                        (second
                          (last
                            (get element :attrs))))))
            (print-attr attr w)))
        (.write w "/>\n")))))

(defn to-xml-script
  [tl script w]
  (let
    [s-xml (xml/parse script)
     tree (tl-file
            (slurp tl))]

    (.write w "<?xml version=\"1.0\" encoding=\"utf-8\"?>")
    (print-xml s-xml tree w)))

(defn batch-insert
  [files]
  (let
    [lm (read-string
          (slurp "lastmodified.dat"))
     new-lm (System/currentTimeMillis)
     fs (filter
          #(>=
             (.lastModified %)
             lm)
          files)
     f-size (count fs)]
    (doseq
      [f fs]
      (printf
        "(%3d/%d) inserting script: %s"
        (inc (.indexOf fs f))
        f-size
        (clojure.string/replace
          (.getName f)
          #".txt"
          ""))
      (with-open
        [w (clojure.java.io/writer
             (format
               "out/%s"
                 (clojure.string/replace
                   (.getName f)
                   #".txt"
                   "")))]
        (let
          [tl (format
                "tl/%s"
                (.getName f))
           scr (format
                 "scripts/%s"
                 (clojure.string/replace
                   (.getName f)
                   #".txt"
                   ""))]
          (to-xml-script tl scr w)))
      (spit "lastmodified.dat" new-lm)
      (println " ... done"))))

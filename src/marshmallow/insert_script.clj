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

(defn write-attr
  [w attr]
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

(declare print-xml)

(defn write-element
  [w tree tag content]
  (.write
    w
    (format "<%s>\n" tag))
  (doseq
    [child content]
    (print-xml child tree w))
  (.write
    w
    (format "</%s>\n" tag)))

(defn write-element-empty
  [w tree tag attrs]
  (when
    (not-empty tag)
    (.write
      w
      (format
        "\t\t<%s"
        (subs tag 1)))
    (doseq
      [attr attrs]
      (case
        (first attr)
        :name (.write
                w
                (format
                  " name=\"%s\""
                  (get-name
                    tree
                    (second attr))))
        :text (.write
                w
                (format
                  " txt=\"%s\""
                  (get-line
                    tree
                    (second
                      (last attrs)))))
        (write-attr w attr)))
    (.write w "/>\n")))

(defn print-xml
  [element tree w]
  (let
    [content (get element :content)]
    (if
      (not-empty content)
      (write-element
        w
        tree
        (subs
          (str
            (get element :tag))
          1)
        content)
      (write-element-empty
        w
        tree
        (str
          (get element :tag))
        (get element :attrs)))))

(defn to-xml-script
  [tl script w]
  (let
    [s-xml (xml/parse script)
     tree (tl-file
            (slurp tl))]

    (.write w "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
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

(def test-tree
  (xml/parse "scripts/花音_01.srcxml"))

(def tl-tree
  (tl-file (slurp "tl/花音_01.srcxml.txt")))

test-tree

tl-tree

(get-line tl-tree "test")


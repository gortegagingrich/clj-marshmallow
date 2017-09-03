(ns marshmallow.parse-script)
(require '[clojure.xml :as xml]
         '[clojure.zip :as zip])

(defn get-attr
  "returns given attribute"
  [x attr]
  (get
    (get x :attrs)
    attr))

(defn get-xscript
  "Assuming consistent structure, returns xscript element in file"
  [file]
  (get
    (->>
      file
      (xml/parse)
      (zip/xml-zip)
      (zip/down)
      (zip/down)
      (second))
    :pnodes))

(defn get-code-rec
  "recursively goes deeper until it finds code element"
  [maps]
  (let [head (first maps)]
    (if
      (=
        (get head :tag)
        :code)
      (get-code-rec
        (get head :content))
      (get head :content))))

(defn get-code
  "finds the code element in the given file"
  [file]
  (get
    (first
      (get-code-rec
        (get-xscript file)))
    :content))

(defn get-messages
  "parses given file as xml and filters out non-messages"
  [file]
  (filter
    (fn [x]
      (=
        (get x :tag)
        :msg))
    (get-code file)))

(defn get-lines
  [messages]
  (if (empty? messages)
    []
    (let [msg (first messages)
          msgs (rest messages)]
    (cons
      (format
        "// speaker: %s\n<jpn %s>%s\n<eng %s>\n\n"
        (get-attr msg :name)
        (get-attr msg :_at_l)
        (get-attr msg :text)
        (get-attr msg :_at_l))
      (get-lines msgs)))))


(defn mod-and-add
  "Generic count function for a set of lines"
  [lines f]
  (if
    (empty? lines)
    0
    (+
      (f lines)
      (mod-and-add
        (rest lines)
        f))))

(defn count-chars
  "Gives a character count for the script's messages"
  [file]
  (mod-and-add
    (get-messages file)
    (fn
      [lines]
      (count
        (get-attr
          (first lines)
          :text)))))

(defn count-bytes-line
  "Return's the given shift-jis string's size in bytes"
  [line]
  (count
    (.getBytes
      (get-attr
        line
        :text)
      "SHIFT-JIS")))

(defn count-bytes
  "Gives a byte count for the script's messages assuming shift-jis encoding"
  [file]
  (mod-and-add
    (get-messages file)
    (fn
      [lines]
      (count-bytes-line
        (first lines)))))

(defn get-spoken
  [msg]
  (filter
    (fn [x]
      (not-empty (get-attr x :name)))
    msg))

(defn get-unique-names
  [msg]
  (if (empty? msg)
    []
    (distinct
      (cons
        (get-attr (first msg) :name)
        (get-unique-names (rest msg))))))

(defn unique-speakers-rec
  [names]
  (if (empty? names)
    ""
    (str
      (format
        "<jpn>%s\n<eng>\n\n"
        (first names))
      (unique-speakers-rec
        (rest names)))))

(defn unique-speakers
  [msg]
  (unique-speakers-rec
    (get-unique-names
      (get-spoken msg))))


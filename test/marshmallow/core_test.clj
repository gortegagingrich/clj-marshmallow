(ns marshmallow.core-test
  (:require [clojure.test :refer :all]
            [marshmallow.core :refer :all]
            [marshmallow.parse-script :refer :all]))

; test functions in parse-script
(deftest parse-script
  "parse-script tests"
  ; sample script
  (def file "豎神02.srcxml")

  ; message elements in script
  (def messages
    (get-messages file))

  ; line count
  (is
    (=
      (count messages)
      209))

  ; byte count
  (is
    (=
      (count-bytes file)
      12254))

  ; character count
  (is
    (=
      (count-chars file)
      6282)))

;   Copyright (c) Alan Thompson. All rights reserved.
;   The use and distribution terms for this software are covered by the Eclipse Public
;   License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) which can be found in the
;   file epl-v10.html at the root of this distribution.  By using this software in any
;   fashion, you are agreeing to be bound by the terms of this license.
;   You must not remove this notice, or any other, from this software.
(ns tst.tupelo.misc
  (:require
    [tupelo.core :as t :refer [spy spyx spyxx it-> rel=]]
    [tupelo.misc :as misc]
    [tupelo.string :as ts]
    [tupelo.test :refer [define-fixture dotest dotest-focus is isnt is= isnt= set= nonblank= testing throws?]]
    )
  (:import [java.lang Byte Integer]) )

(dotest
  (when (= :linux (misc/get-os))
    (println "***** found linux ***** ")

    ; ***** kills test-refresh *****
    #_(let [result (misc/shell-cmd "ls -ldF *")]
        (when false ; set true -> debug print
          (println "(:out result)")
          (println (:out result)))
        (is= 0 (:exit result)))

    ; ***** kills test-refresh *****
    #_(let [result (misc/shell-cmd "ls /bin/bash")]
        (is= 0 (:exit result))
        (is= 1 (count (re-seq #"/bin/bash" (:out result)))))

    ; ***** OK for test-refresh *****
    (binding [misc/*os-shell* "/bin/sh"]
      (let [result (misc/shell-cmd "ls /bin/*sh")]
        ;(println :result result)
        (is= 0 (:exit result))
        (is (pos? (count (re-seq #"/bin/bash" (:out result)))))))

    ; ***** OK for test-refresh *****
    (binding [misc/*os-shell* "/bin/tcsh"]
      (let [result (misc/shell-cmd "ls /bin/*sh")]
       ;(println :result result)
        (is= 0 (:exit result))
        (is (pos? (count (re-seq #"/bin/bash" (:out result)))))))

    ; ***** kills test-refresh *****
    #_(do
        (throws? RuntimeException (misc/shell-cmd "LLLls -ldF *")))

    (println "***** leaving linux ***** ")))















(ns clojurewerkz.titanium.conf
  (:import (org.apache.commons.io FileUtils))
  (:require [clojurewerkz.titanium.edges :as te]
            [clojurewerkz.titanium.vertices :as tv]
            [clojurewerkz.titanium.graph :as tg]))


(def cs-dir (str (clojure.java.io/as-url
              (clojure.java.io/as-file
                (str (System/getProperty "user.dir")
                  "/resources/test-cassandra.yaml")))))

(def conf {;; Embedded cassandra settings
           "storage.backend"  "embeddedcassandra"
           "storage.cassandra-config-dir" cs-dir
           ;; Embedded elasticsearch settings
           "storage.index.search.backend" "elasticsearch"
           "storage.index.search.directory" "/tmp/cassandra/elasticsearch"
           "storage.index.search.client-only" false
           "storage.index.search.local-mode" true
           "storage.compression.enabled" false
           "storage.cassandra.db.compression.enabled" false})

(defn clear-db []
  (tg/transact!
    (doseq [e (te/get-all-edges)]
      (te/remove! e))
    (doseq [v (tv/get-all-vertices)]
      (tv/remove! v))))

(defn remove-db []
  (FileUtils/deleteDirectory (java.io.File. "/tmp/titanium-test")))

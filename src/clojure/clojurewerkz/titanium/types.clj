;; Copyright (c) 2013-2014 Michael S. Klishin, Alex Petrov, Zack Maril, and The ClojureWerkz
;; Team
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns clojurewerkz.titanium.types
  (:import (com.thinkaurelius.titan.core TitanType TypeMaker$UniquenessConsistency)
           (com.tinkerpop.blueprints Vertex Edge Direction Graph))
  (:use [clojurewerkz.titanium.graph :only (get-graph ensure-graph-is-transaction-safe)]))

(defn get-type
  [tname]
  (ensure-graph-is-transaction-safe)
  (.getType (get-graph) (name tname)))

(defn- convert-bool-to-lock
  [b]
  (if b
    TypeMaker$UniquenessConsistency/LOCK
    TypeMaker$UniquenessConsistency/NO_LOCK))

(defn deflabel
  "Creates a edge label with the given properties."
  ([tname] (deflabel tname {}))
  ([tname {:keys [simple direction primary-key signature
                  unique-direction unique-locked]
           :or {direction "directed"
                primary-key nil
                signature   nil
                type :m->m ; :m->1 :1->1 :1->m
                unique-locked    true}}]
     (ensure-graph-is-transaction-safe)
     (let [type-maker (.. (get-graph)
                          (makeLabel (name (name tname))))]
      (case type
        :m->m (.manyToMany type-maker)
        :m->1 (.manyToOne  type-maker (convert-bool-to-lock unique-locked))
        :1->1 (.oneToOne   type-maker (convert-bool-to-lock unique-locked))
        :1->m (.oneToMany  type-maker (convert-bool-to-lock unique-locked))
        nil)
       (case direction
         "directed"    (.directed type-maker)
         "unidirected" (.unidirected type-maker))
       (when signature (.signature type-maker signature))
       (when primary-key (.sortKey type-maker (into-array TitanType primary-key)))
       (.make type-maker))))

(defn defkey
  "Creates a property key with the given properties."
  ([tname data-type] (defkey tname data-type {}))
  ([tname data-type {:keys [type ; :unique / :single / nil ; TODO add support for Keymaker.list
                            unique-locked
                            indexed-vertex?
                            indexed-edge?
                            searchable?]
                     :or   {type          nil
                            unique-locked true}}]
     (ensure-graph-is-transaction-safe)
     (let [type-maker   (.. (get-graph)
                            (makeKey (name (name tname)))
                            (dataType data-type))]
       (when indexed-vertex?
         (if searchable?
           (.indexed type-maker "search" Vertex)
           (.indexed type-maker Vertex)))
       (when indexed-edge?
         (if searchable?
           (.indexed type-maker "search" Edge)
           (.indexed type-maker Edge)))

       (case type
        :unique (.unique type-maker (convert-bool-to-lock unique-locked))
        :single (.single type-maker (convert-bool-to-lock unique-locked))
        :list   (.list type-maker)
        nil)
       (.make type-maker))))

(defn deflabel-once
  "Checks to see if a edge label with the given name exists already.
  If so, nothing happens, otherwise it is created."
  ([tname] (deflabel-once tname {}))
  ([tname m]
     (ensure-graph-is-transaction-safe)
     (if-let [named-type (get-type tname)]
       named-type
       (deflabel tname m))))

(defn defkey-once
  "Checks to see if a property key with the given name exists already.
  If so, nothing happens, otherwise it is created."
  ([tname data-type] (defkey-once tname data-type {}))
  ([tname data-type m]
     (ensure-graph-is-transaction-safe)
     (if-let [named-type (get-type tname)]
       named-type
       (defkey tname data-type m))))

(ns habits.backend.entity.user-entity)

(defn build [from-db]
  (println from-db)
  {
   "id" (:users/id from-db)
   "name" (:users/name from-db)
   })

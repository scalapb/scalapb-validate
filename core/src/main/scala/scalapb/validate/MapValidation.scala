package scalapb.validate

import io.envoyproxy.pgv.ValidationException

object MapValidation {
  val SPARSE_MAPS_NOT_SUPPORTED = "Sparse maps are not supported"

  def maxPairs[K, V](name: String, v: Map[K, V], limit: Int) =
    Result(
      v.size <= limit,
      new ValidationException(
        name,
        v,
        s"$v must have at most $limit pairs"
      )
    )

  def minPairs[K, V](name: String, v: Map[K, V], limit: Int) =
    Result(
      v.size >= limit,
      new ValidationException(
        name,
        v,
        s"$v must have at least $limit pairs"
      )
    )

  def notSparse[K, V](name: String, v: Map[K, V]) =
    Failure(new ValidationException(name, v, SPARSE_MAPS_NOT_SUPPORTED))
}

package scalapb.validate

import io.envoyproxy.pgv.ValidationException

object MapValidation {
  val SPARSE_MAPS_NOT_SUPPORTED = "Sparse maps are not supported"

  def maxPairs[K, V](name: String, v: Map[K, V], limit: Int): Result =
    maxPairs(name, v.size, limit)

  def minPairs[K, V](name: String, v: Map[K, V], limit: Int): Result =
    minPairs(name, v.size, limit)

  def maxPairs[K, V](name: String, actualSize: Int, limit: Int): Result =
    Result(
      actualSize <= limit,
      new ValidationException(
        name,
        actualSize,
        s"Expected map to have at most $limit pair, got $actualSize"
      )
    )

  def minPairs[K, V](name: String, actualSize: Int, limit: Int): Result =
    Result(
      actualSize >= limit,
      new ValidationException(
        name,
        actualSize,
        s"Expected map to have at least $limit pair, got $actualSize"
      )
    )

  def notSparse[K, V](name: String, v: Map[K, V]) =
    Failure(new ValidationException(name, v, SPARSE_MAPS_NOT_SUPPORTED))
}

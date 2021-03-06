package scalapb.validate

object MapValidation {
  val SPARSE_MAPS_NOT_SUPPORTED = "Sparse maps are not supported"

  def maxPairs[K, V](name: String, v: Map[K, V], limit: Int): Result =
    maxPairs(name, v.size, limit)

  def minPairs[K, V](name: String, v: Map[K, V], limit: Int): Result =
    minPairs(name, v.size, limit)

  def maxPairs[K, V](name: String, actualSize: Int, limit: Int): Result =
    Result(
      actualSize <= limit,
      ValidationFailure(
        name,
        actualSize,
        s"Expected map to have at most $limit pair, got $actualSize"
      )
    )

  def minPairs[K, V](name: String, actualSize: Int, limit: Int): Result =
    Result(
      actualSize >= limit,
      ValidationFailure(
        name,
        actualSize,
        s"Expected map to have at least $limit pair, got $actualSize"
      )
    )

  def notSparse[K, V](name: String, v: Map[K, V]) =
    Failure(ValidationFailure(name, v, SPARSE_MAPS_NOT_SUPPORTED))
}

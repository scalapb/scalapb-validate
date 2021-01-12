package scalapb.transforms.refined

import scalapb.TypeMapper
import eu.timepit.refined.refineV
import eu.timepit.refined.api.{Refined, Validate}
import scalapb.validate.ValidationException

package object refined {
  implicit def refinedType[T, V](implicit ev: Validate[T, V]) =
    TypeMapper[T, Refined[T, V]](refineV(_) match {
      case Left(error)  => throw new ValidationException(error)
      case Right(value) => value
    })(_.value)

}

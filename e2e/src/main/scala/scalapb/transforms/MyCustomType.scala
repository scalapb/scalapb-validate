package scalapb.transforms

import scalapb.TypeMapper

import scalapb.transforms.field.MyMsg

final case class MyCustomType(a: String)

object MyCustomType {
  implicit val tm =
    TypeMapper[MyMsg, MyCustomType](pb => MyCustomType(pb.a))(custom =>
      MyMsg(custom.a)
    )
}

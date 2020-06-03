package com.thesamet.scalapb.validate

trait Validator[T] {
  def validate(t: T): Boolean
}

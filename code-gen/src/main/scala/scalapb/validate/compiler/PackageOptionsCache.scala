package scalapb.validate.compiler

import com.google.protobuf.Descriptors.FileDescriptor
import scalapb.validate.Validate.PackageOptions
import scalapb.compiler.GeneratorException

class PackageOptionsCache(cache: Map[String, PackageOptions]) {
  def get(packageName: String): PackageOptions =
    (packageName :: PackageOptionsCache.parentPackages(packageName))
      .find(cache.contains)
      .flatMap(cache.get)
      .getOrElse(PackageOptions.getDefaultInstance())
}

object PackageOptionsCache {
  private def parentPackages(packageName: String): List[String] =
    packageName
      .split('.')
      .scanLeft(Seq[String]())(_ :+ _)
      .drop(1)
      .dropRight(1)
      .map(_.mkString("."))
      .reverse
      .toList

  def from(protos: Seq[FileDescriptor]): PackageOptionsCache = {
    val givenPackageOptions = protos.collect {
      case proto
          if (proto
            .getOptions()
            .hasExtension(
              scalapb.validate.Validate.package_
            ) && proto.getPackage.nonEmpty) =>
        proto.getPackage() -> proto
          .getOptions()
          .getExtension(scalapb.validate.Validate.package_)
    }.sortBy(_._1.length)

    givenPackageOptions.groupBy(_._1).find(_._2.length > 1).foreach { p =>
      throw new GeneratorException(
        s"Preprocessor options for package '${p._1}' found in more than one proto file."
      )
    }

    // Merge package-scoped options of parent packages with sub-packages, so for each package it
    // is sufficient to look up the nearest parent package that has package-scoped options.
    val optionsByPackage =
      new collection.mutable.HashMap[String, PackageOptions]

    givenPackageOptions.foreach { case pso =>
      val parents: List[String] = parentPackages(pso._1)
      val actualOptions = parents.find(optionsByPackage.contains) match {
        case Some(p) =>
          optionsByPackage(p).toBuilder().mergeFrom(pso._2).build()
        case None => pso._2
      }
      optionsByPackage += pso._1 -> actualOptions
    }
    new PackageOptionsCache(optionsByPackage.toMap)
  }
}

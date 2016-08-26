package iliad
package algebra

import shapeless._
import shapeless.ops.nat._

import org.scalacheck._

object MatrixGen {
  def gen[N <: Nat, M <: Nat, A](genA: Gen[A])(implicit toIntN: ToInt[N], toIntM: ToInt[M]): Gen[Matrix[N, M, A]] =
    Gen.listOfN(toIntN() * toIntM(), genA).map(as => Matrix.sized[A, N, M](as.toVector))

  def symmetric[N <: Nat, A](genA: Gen[A])(implicit toIntN: ToInt[N]): Gen[Matrix[N, N, A]] = {
    val N = toIntN()
    for {
      diag <- Gen.listOfN(N, genA)
      els <- Gen.listOfN(N * (N - 1), genA)
    } yield {
      val builder = Vector.newBuilder[A]
      (0 until N).foreach { i =>
        (0 until N).foreach { j =>
          if(i == j)
            builder += diag(i)
          else {
            val n = (i + j) % els.size
            builder += els(n)
          }
        }
      }
      val m = Matrix.sized[A, N, N](builder.result())
      builder.clear()
      m
    }
  }
}

object OrthoMatrixGen {
  def gen[N <: Nat, M <: Nat, A](genA: Gen[A])(implicit toIntN: ToInt[N], toIntM: ToInt[M]): Gen[OrthoMatrix[N, M , A]] = MatrixGen.gen[N, M, A](genA).map(OrthoMatrix(_))
}
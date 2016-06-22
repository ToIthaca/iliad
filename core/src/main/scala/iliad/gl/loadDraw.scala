package iliad
package gl

import iliad.kernel.platform.GLES30Library

import cats._
import cats.data._
import cats.free._
import cats.implicits._

import freek._

import monocle.function.all._
import monocle.std.all._

import FreekExtra._
import MonocleExtra._

object LoadDraw {

  type LoadDraw[A] =
    (Load :|: Cached :|: Draw :|: Current :|: GL :|: FXNil)#Cop[A]
  type DSL[A] = Free[LoadDraw, A]
  type PRG[F[_], A] =
    ReaderT[StateT[F, (Cached.State, Current.State), ?], GLES30Library, A]

  private def liftOpenGL[F[_]: Monad]: GL.Effect[F, ?] ~> PRG[F, ?] =
    new (GL.Effect[F, ?] ~> PRG[F, ?]) {
      def apply[A](eff: ReaderT[F, GLES30Library, A]): PRG[F, A] =
        eff.mapF(_.liftT[StateT[?[_], (Cached.State, Current.State), ?]])
    }

  def liftS[F[_]: Monad, A](
      s: State[(Cached.State, Current.State), A]): PRG[F, A] =
    ReaderT(_ => s.transformF(sa => Applicative[F].pure(sa.value)))

  private def liftCached[F[_]: Monad]: Cached.Effect ~> PRG[F, ?] =
    new (Cached.Effect ~> PRG[F, ?]) {
      def apply[A](eff: State[Cached.State, A]): PRG[F, A] =
        liftS(eff.applyLens[(Cached.State, Current.State)](first))
    }

  private def liftCurrent[F[_]: Monad]: Current.Effect ~> PRG[F, ?] =
    new (Current.Effect ~> PRG[F, ?]) {
      def apply[A](eff: State[Current.State, A]): PRG[F, A] =
        liftS(eff.applyLens[(Cached.State, Current.State)](second))
    }

  def runner[F[_]: Monad](
      f: GL.Interpreter[GL.Effect[F, ?]]): Interpreter[LoadDraw, PRG[F, ?]] =
    Load.parse(f).andThen(liftOpenGL) :&:
    CachedParser.andThen(liftCached[F]) :&:
    Draw.parse(f).andThen(liftOpenGL) :&:
    CurrentParser.andThen(liftCurrent[F]) :&:
    f.andThen(liftOpenGL)

  private def load(s: VertexShader.Source): DSL[VertexShader.Compiled] =
    Cached.get(s).freekF[LoadDraw] flatMap {
      case Some(v) => Free.pure(v)
      case None =>
        for {
          v <- Load(s).freekF[LoadDraw]
          _ <- Cached.put(v).freekF[LoadDraw]
        } yield v
    }

  private def load(s: FragmentShader.Source): DSL[FragmentShader.Compiled] =
    Cached.get(s).freekF[LoadDraw] flatMap {
      case Some(v) => Free.pure(v)
      case None =>
        for {
          v <- Load(s).freekF[LoadDraw]
          _ <- Cached.put(v).freekF[LoadDraw]
        } yield v
    }

  //TODO: set current program as ths one
  def load(p: Program.Unlinked): DSL[Program.Linked] =
    Cached.get(p).freekF[LoadDraw] flatMap {
      case Some(p) => Free.pure(p)
      case None =>
        for {
          v <- load(p.vertex)
          f <- load(p.fragment)
          pl <- Load(v, f).freekF[LoadDraw]
          _ <- Cached.put(pl).freekF[LoadDraw]
        } yield pl
    }

  def load(r: VertexData.Ref,
           d: VertexData.Data,
           pageSize: Int): DSL[Unit] =
    Cached.get(r.buffer).freekF[LoadDraw] flatMap {
      case Some(prev) =>
        if (VertexBuffer.fits(prev, d.size))
          for {
            next <- Load.insert(r, d, pageSize, prev).freekF[LoadDraw]
            _ <- Cached.put(next).freekF[LoadDraw]
          } yield ()
        else
          for {
            next <- Load.copy(r, d, pageSize, prev).freekF[LoadDraw]
            _ <- Cached.put(next).freekF[LoadDraw]
          } yield ()
      case None =>
        for {
          b <- Load.create(r, d, pageSize, r.buffer).freekF[LoadDraw]
          _ <- Cached.put(b).freekF[LoadDraw]
        } yield ()
    }

  def load(r: ElementData.Ref,
           d: ElementData.Data,
           pageSize: Int): DSL[Unit] =
    Cached.get(r.buffer).freekF[LoadDraw] flatMap {
      case Some(prev) =>
        if (ElementBuffer.fits(prev, d.size))
          for {
            next <- Load.insert(r, d, pageSize, prev).freekF[LoadDraw]
            _ <- Cached.put(next).freekF[LoadDraw]
          } yield ()
        else
          for {
            next <- Load.copy(r, d, pageSize, prev).freekF[LoadDraw]
            _ <- Cached.put(next).freekF[LoadDraw]
          } yield ()
      case None =>
        for {
          b <- Load.create(r, d, pageSize, r.buffer).freekF[LoadDraw]
          _ <- Cached.put(b).freekF[LoadDraw]
        } yield ()
    }

  def clear(bitMask: ChannelBitMask): DSL[Unit] =
    Draw.clear(bitMask).freekF[LoadDraw]

  private def doIfNot(f: Current.DSL[Boolean])(g: DSL[Unit]): DSL[Unit] =
    f.freekF[LoadDraw] flatMap (b => if (b) Free.pure(()) else g)

  private def setFramebuffer(framebuffer: Int): DSL[Unit] =
    doIfNot(Current.contains(framebuffer))(
        Draw.bindFramebuffer(framebuffer).freekF[LoadDraw] >> Current
          .set(framebuffer)
          .freekF[LoadDraw]
    )

  private def set(p: Program.Linked): DSL[Unit] = doIfNot(Current.contains(p))(
      Draw.use(p).freekF[LoadDraw] >> Current.set(p).freekF[LoadDraw]
  )

  private def set(vb: VertexBuffer.Loaded): DSL[Unit] =
    doIfNot(Current.contains(vb))(
        Draw.bind(vb).freekF[LoadDraw] >> Current.set(vb).freekF[LoadDraw]
    )

  private def set(eb: ElementBuffer.Loaded): DSL[Unit] =
    doIfNot(Current.contains(eb))(
        Draw.bind(eb).freekF[LoadDraw] >> Current.set(eb).freekF[LoadDraw]
    )

  private def ensure[A](
      c: Cached.DSL[Option[A]], msg: String): XorT[DSL, String, A] =
    XorT(Cached.ensure(c, msg).freekF[LoadDraw])

  private def xort[A](dsl: DSL[A]): XorT[DSL, String, A] = XorT.right(dsl)

  private def draw(draw: DrawOp): DSL[String Xor Unit] =
    (for {
      _ <- xort(setFramebuffer(draw.framebuffer))
      p <- ensure(Cached.get(draw.program),
                  s"Program not loaded. Unable to draw $draw")
      _ <- xort(set(p))
      vb <- ensure(Cached.get(draw.vertexBuffer),
                   s"Vertex buffer not loaded. Unable to draw $draw")
      _ <- xort(set(vb))
      vd <- ensure(Cached.get(draw.vertexData),
                   s"Vertex data not loaded. Unable to draw $draw")
      eb <- ensure(Cached.get(draw.elementBuffer),
                   s"Element buffer not loaded. Unable to draw $draw")
      _ <- xort(set(eb))
      ed <- ensure(Cached.get(draw.elementData),
                   s"Element data not loaded. Unable to draw $draw")
      as <- XorT.fromXor[DSL](p.loaded(draw.attributes))
      _ <- xort(Draw.enable(as, vd.offset(draw.vertexModel)).freekF[LoadDraw])
      _ <- xort(Draw(ed.offset(draw.elementModel)).freekF[LoadDraw])
    } yield ()).value
}

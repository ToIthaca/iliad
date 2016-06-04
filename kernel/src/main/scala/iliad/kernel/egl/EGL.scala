package iliad
package kernel
package egl

import iliad.kernel.platform.egl.Lib

import scala.reflect._

import cats._
import cats.data._
import cats.implicits._

object EGL {

  import Constants._

  /** Effect type of Debugging runner */
  type Debugger[F[_], A] = XorT[F, String, A]

  /** Effect type of Logging runner */
  type Logger[F[_], A] = WriterT[F, List[String], A]

  /** Major and minor EGL versions for the display */
  case class DisplayVersion(major: Int, minor: Int)


  /** Attribute constructors */
  def configValue(i: Int): Int Xor Constants.ConfigAttribValue = i.left
  def configValue(i: Constants.ConfigAttribValue): Int Xor Constants.ConfigAttribValue = i.right
  def contextValue(i: Int): Int Xor Constants.ContextAttribValue = i.left
  def pbufferValue(i: Int): Int Xor PBufferAttribValue = i.left
  def pbufferValue(i: PBufferAttribValue): Int Xor PBufferAttribValue = i.right

  case class Attributes[K <: IntConstant, V <: IntConstant](attributes: Map[K, Int Xor V]) {
    private def attribList(as: Map[Constants.IntConstant, Xor[Int, Constants.IntConstant]]): Array[Int] = {
      (as.flatMap { case (key, xor) =>  Seq(key.value, xor.map(_.value).merge) }.toSeq :+ Constants.EGL_NONE.value).toArray
    }

    /** Produces an array of the form key, value, key, value ... EGL_NONE */
    def toArray: Array[Int] = attribList(attributes.map(identity))
  }

  type ConfigAttributes = Attributes[ConfigAttrib, ConfigAttribValue]
  type ContextAttributes = Attributes[ContextAttrib, ContextAttribValue]
  type WindowAttributes = Attributes[WindowAttrib, WindowAttribValue]
  type PBufferAttributes = Attributes[PBufferAttrib, PBufferAttribValue]

  /** Session returned by context creation */
  case class Session[Disp, Cfg, Ctx](display: Disp, config: Cfg, context: Ctx)

  /** EGL runners */
  def noEffectsRunning[NDisp, NWin, Disp, Cfg: ClassTag, Sfc, Ctx]: EGL[Id, NDisp, NWin, Disp, Cfg, Sfc, Ctx] = new NoEffectsRunning[NDisp, NWin, Disp, Cfg, Sfc, Ctx]()
  def debugging[NDisp, NWin, Disp, Cfg: ClassTag, Sfc, Ctx]: EGL[Debugger[Id, ?], NDisp, NWin, Disp, Cfg, Sfc, Ctx] = new Debugging(noEffectsRunning[NDisp, NWin, Disp, Cfg, Sfc, Ctx])
  def logging[NDisp, NWin, Disp, Cfg: ClassTag, Sfc, Ctx]: EGL[Logger[Id, ?], NDisp, NWin, Disp, Cfg, Sfc, Ctx] = new Logging(noEffectsRunning[NDisp, NWin, Disp, Cfg, Sfc, Ctx])
  def debuggingLogging[NDisp, NWin, Disp, Cfg: ClassTag, Sfc, Ctx]: EGL[Debugger[Logger[Id, ?], ?], NDisp, NWin, Disp, Cfg, Sfc, Ctx] = new Debugging(new Logging(new NoEffectsRunning[NDisp, NWin, Disp, Cfg, Sfc, Ctx]()))
}

/** EGL API to be implemented by EGL runners.
  * 
  * Type parameters are mapped as follows:
  *   NDisp corresponds to [[iliad.kernel.platform.egl.Lib.EGLNativeDisplayType]]
  *   NWin  corresponds to [[iliad.kernel.platform.egl.Lib.EGLNativeWindowType]]
  *   Disp  corresponds to [[iliad.kernel.platform.egl.Lib.EGLDisplay]]
  *   Cfg   corresponds to [[iliad.kernel.platform.egl.Lib.EGLConfig]]
  *   Sfc   corresponds to [[iliad.kernel.platform.egl.Lib.EGLSurface]]
  *   Ctx   corresponds to [[iliad.kernel.platform.egl.Lib.EGLContext]]
  */
abstract class EGL[F[_]: Monad, NDisp, NWin, Disp, Cfg : ClassTag, Sfc, Ctx] {
  import EGL._
  import Constants._

  type EGLLib = Lib.Aux[NDisp, NWin, Disp, Cfg, Sfc, Ctx]
  type IO[A] = ReaderT[F, EGLLib, A]

  def getError: IO[Int]
  private[egl] def getConfigAttrib(display: Disp, config: Cfg, attribute: FixedConfigAttrib): IO[Int]
  def getEnumConfigAttrib(display: Disp, config: Cfg, attribute: EnumConfigAttrib): IO[Int Xor ConfigAttribValue] = getConfigAttrib(display, config, attribute).map( v => 
    Xor.fromOption(SealedEnum.values[ConfigAttribValue].find(_.value == v), v)
  )
  def getIntConfigAttrib(display: Disp, config: Cfg, attribute: IntConfigAttrib): IO[Int] = getConfigAttrib(display, config, attribute)

  def queryString(dpy: Disp, name: QueryKey): IO[String]

  def getDisplay(displayID: NDisp): IO[Disp]
  private[egl] def initialise(display: Disp): IO[(Int, Int)]
  def initialisedVersion(display: Disp): IO[DisplayVersion] = initialise(display).map(t => DisplayVersion(t._1, t._2))

  def chooseConfig(display: Disp, attributes: ConfigAttributes): IO[Cfg]
  def createWindowSurface(display: Disp, config: Cfg, win: NWin, attributes: WindowAttributes): IO[Sfc]
  def createPbufferSurface(display: Disp, config: Cfg, attributes: PBufferAttributes): IO[Sfc]
  
  private[egl] def bindApi(api: API): IO[Unit]
  def bindOpenGLESApi: IO[Unit] = bindApi(EGL_OPENGL_ES_API)
  
  private[egl] def createContext(display: Disp, config: Cfg, shareContext: Ctx, attributes: ContextAttributes): IO[Ctx]
  def primaryContext(display: Disp, config: Cfg, noContext: Ctx, attributes: ContextAttributes): IO[Ctx] = createContext(display, config, noContext, attributes)
  def secondaryContext(display: Disp, config: Cfg, primaryContext: Ctx, attributes: ContextAttributes): IO[Ctx] = createContext(display, config, primaryContext, attributes)

  def setupPrimaryContext(displayID: NDisp, windowID: NWin, noContext: Ctx): IO[Session[Disp, Cfg, Ctx]] = for {
    display <- getDisplay(displayID)
    _ <- initialisedVersion(display)
    config <- chooseConfig(display, Defaults.primaryConfigAttributes)
    surface <- createWindowSurface(display, config, windowID, Defaults.windowAttributes)
    _ <-bindOpenGLESApi
    context <-primaryContext(display, config, noContext, Defaults.contextAttributes)
  } yield Session(display, config, context)

  def setupSecondaryContext(session: Session[Disp, Cfg, Ctx]): IO[Ctx] = for {
    _ <- initialisedVersion(session.display)
    config <- chooseConfig(session.display, Defaults.secondaryConfigAttributes)
    surface <- createPbufferSurface(session.display, config, Defaults.pBBufferAttributes)
    _ <- bindOpenGLESApi
    context <- secondaryContext(session.display, config, session.context, Defaults.contextAttributes)
  } yield context
}

object Defaults {
  import EGL._
  import Constants._

  def primaryConfigAttributes: ConfigAttributes = Attributes[ConfigAttrib, ConfigAttribValue](Map(
    EGL_LEVEL -> configValue(0),
    EGL_SURFACE_TYPE -> configValue(EGL_WINDOW_BIT),
    EGL_RENDERABLE_TYPE -> configValue(EGL_OPENGL_ES3_BIT),
    EGL_BLUE_SIZE -> configValue(8),
    EGL_GREEN_SIZE -> configValue(8),
    EGL_RED_SIZE -> configValue(8),
    EGL_ALPHA_SIZE -> configValue(8),
    EGL_BUFFER_SIZE -> configValue(32),
    EGL_DEPTH_SIZE -> configValue(24)
  ))

  def contextAttributes: ContextAttributes = Attributes[ContextAttrib, ContextAttribValue](Map(
    EGL_CONTEXT_CLIENT_VERSION -> contextValue(3)
  ))

  def windowAttributes: WindowAttributes = Attributes[WindowAttrib, WindowAttribValue](Map.empty)

  def secondaryConfigAttributes: ConfigAttributes = Attributes[ConfigAttrib, ConfigAttribValue](Map(
    EGL_SAMPLES -> configValue(1),
    EGL_SURFACE_TYPE -> configValue(EGL_PBUFFER_BIT),
    EGL_RENDERABLE_TYPE -> configValue(EGL_OPENGL_ES3_BIT),
    EGL_BLUE_SIZE -> configValue(8),
    EGL_GREEN_SIZE -> configValue(8),
    EGL_RED_SIZE -> configValue(8),
    EGL_ALPHA_SIZE -> configValue(0),
    EGL_BUFFER_SIZE -> configValue(32),
    EGL_DEPTH_SIZE -> configValue(16)
  ))
 
  def pBBufferAttributes: PBufferAttributes = Attributes[PBufferAttrib, PBufferAttribValue](Map(
    EGL_WIDTH -> pbufferValue(1),
    EGL_HEIGHT -> pbufferValue(1),
    EGL_TEXTURE_FORMAT -> pbufferValue(EGL_TEXTURE_RGBA),
    EGL_TEXTURE_TARGET -> pbufferValue(EGL_TEXTURE_2D)
  ))
}

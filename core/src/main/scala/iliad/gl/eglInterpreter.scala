package iliad
package gl

import scala.reflect.ClassTag

import iliad.kernel.platform.EGL14Library
import iliad.kernel.Buffer //TODO: move this to a different place

import cats._
import cats.data._

final class EGLInterpreter[NDisp, NWin, Disp, Cfg : ClassTag, Sfc, Ctx]
    extends (EGL[NDisp, NWin, Disp, Cfg, Sfc, Ctx, ?] ~> Reader[EGL14Library.Aux[NDisp, NWin, Disp, Cfg, Sfc, Ctx], ?]) {

  def reader[A](f: EGL14Library.Aux[NDisp, NWin, Disp, Cfg, Sfc, Ctx] => A): Reader[EGL14Library.Aux[NDisp, NWin, Disp, Cfg, Sfc, Ctx], A] = Reader(f)

  def apply[A](egl: EGL[NDisp, NWin, Disp, Cfg, Sfc, Ctx, A]):
      Reader[EGL14Library.Aux[NDisp, NWin, Disp, Cfg, Sfc, Ctx], A] = egl match {
    case EGLGetError => reader(_.eglGetError)
    case EGLChooseConfig(dpy, attrs, count) => reader { lib =>
      val s = Buffer.capacity[Int](1)
      val cfgs = new Array[Cfg](count)
      lib.eglChooseConfig(dpy, attrs.toArray, cfgs, count, s)
      val size = s.get()
      @scala.annotation.tailrec
      def go(num: Int, xs: List[Cfg]): List[Cfg] = {
        if(num > 0) go(num - 1, cfgs(num - 1) :: xs) else xs
      }
      (go(size, List.empty).reverse, size)
    }
    case EGLQueryString(disp, p) => reader(_.eglQueryString(disp, p.value))
    case EGLCreateContext(disp, cfg, sc, attribs) =>
      //explicit cast because type isn't inferred
      reader(_.eglCreateContext(disp, cfg, sc, attribs.toArray)).asInstanceOf
    case EGLBindAPI(api) => reader(_.eglBindAPI(api.value))
    case EGLCreateWindowSurface(disp, cfg, nw, attribs) =>
      //explicit cast because type isn't inferred      
      reader(_.eglCreateWindowSurface(disp, cfg, nw, attribs.toArray)).asInstanceOf
    case EGLGetDisplay(nDisp) =>
      //explicit cast because type isn't inferred
      reader(_.eglGetDisplay(nDisp)).asInstanceOf
    case EGLSwapBuffers(disp, sfc) => reader(_.eglSwapBuffers(disp, sfc))
    case EGLMakeCurrent(disp, draw, read, ctx) => reader(_.eglMakeCurrent(disp, draw, read, ctx))
    case EGLInitialize(disp) => reader { lib =>
      val mj = Buffer.capacity[Int](1)
      val mn = Buffer.capacity[Int](1)
      lib.eglInitialize(disp, mj, mn)
      (mj.get(), mn.get())
    }
    case EGLGetConfigAttrib(dpy, cfg, attr) => reader { lib =>
      val value = Buffer.capacity[Int](1)
      lib.eglGetConfigAttrib(dpy, cfg, attr.value, value)
      value.get()
    }
    case EGLSwapInterval(dpy, interval) => reader(_.eglSwapInterval(dpy, interval))
    //explit cast because type isn't inferred  
    case EGL_DEFAULT_DISPLAY() => reader(_.EGL_DEFAULT_DISPLAY).asInstanceOf
    case EGL_NO_DISPLAY() => reader(_.EGL_NO_DISPLAY).asInstanceOf
    case EGL_NO_CONTEXT() => reader(_.EGL_NO_CONTEXT).asInstanceOf
    case EGL_NO_SURFACE() => reader(_.EGL_NO_SURFACE).asInstanceOf
  }
}

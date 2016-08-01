package iliad
package kernel

import android.app.Activity
import android.view.{GestureDetector, MotionEvent}
import android.support.v4.view.GestureDetectorCompat

import EventHandler._

import com.typesafe.scalalogging._

trait AndroidEventHandler extends Activity with GestureDetector.OnGestureListener
    with GestureDetector.OnDoubleTapListener
    with EventHandler with LazyLogging {

  def width: Int
  def height: Int

  private var eventCallback: Callback[InputEvent] = EventHandler.zero

  var detector: GestureDetectorCompat = _
  var recogniser: EventRecogniser = _

  def onEvent(cb: Callback[InputEvent]): Unit = eventCallback = cb

  override def onTouchEvent(event: MotionEvent): Boolean = {
    logger.debug(s"OnTouchEvent: $event")
    detector.onTouchEvent(event)      
    if(event.getAction() == MotionEvent.ACTION_UP) {
      val (n, e) = recogniser.onActionUp(width, height)
      recogniser = n
      e.foreach(eventCallback)
    }
    super.onTouchEvent(event)
  }

  override def onDown(event: MotionEvent): Boolean =  {
    logger.debug("onDown: " + event.toString());
    return true;
  }

  override def onFling(event1: MotionEvent, event2: MotionEvent, velocityX: Float, velocityY: Float): Boolean = {
    logger.debug("onFling: " + event1.toString()+event2.toString());
    val (n, e) = recogniser.onFling(event1, event2)(width, height)
    recogniser = n
    e.foreach(eventCallback)
    return true;
  }

  override def onLongPress(event: MotionEvent): Unit = {
    logger.debug("onLongPress: " + event.toString());
  }

  override def onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean = {
    logger.debug("onScroll: " + e1.toString()+e2.toString());
    val (n, e) = recogniser.onScroll(e1, e2)(width, height)
    recogniser = n
    e.foreach(eventCallback)
    return true;
  }

  override def onShowPress(event: MotionEvent): Unit = {
    logger.debug("onShowPress: " + event.toString());
  }

  override def onSingleTapUp(event: MotionEvent): Boolean = {
    logger.debug("onSingleTapUp: " + event.toString());
    val (n, e) = recogniser.onTap(event)(width, height)
    recogniser = n
    e.foreach(eventCallback)
    return true;
  }

  override def onDoubleTap(event: MotionEvent): Boolean = {
    logger.debug("onDoubleTap: " + event.toString());
    return true;
  }

  override def onDoubleTapEvent(event: MotionEvent): Boolean = {
    logger.debug("onDoubleTapEvent: " + event.toString());
    return true;
  }

  override def onSingleTapConfirmed(event: MotionEvent): Boolean = {
    logger.debug("onSingleTapConfirmed: " + event.toString());
    return true;
  }
}

sealed trait EventRecogniser {
  def onTap(event: MotionEvent)(width: Int, height: Int): 
      (EventRecogniser, Option[InputEvent])
  def onScroll(event1: MotionEvent, event2: MotionEvent)(width: Int, height: Int): 
      (EventRecogniser, Option[InputEvent])
  def onFling(event1: MotionEvent, event2: MotionEvent)(width: Int, height: Int): 
      (EventRecogniser, Option[InputEvent])
  def onActionUp(width: Int, height: Int): (EventRecogniser, Option[InputEvent])
}

object EventRecogniser {

  private def point(e: MotionEvent, width: Int, height: Int): InputEvent.Point = {
    val x = e.getX.toFloat / width.toFloat
    val y = 1f - (e.getY.toFloat / height.toFloat)
    InputEvent.Point(System.currentTimeMillis, x, y)
  }

  case object Blank
      extends EventRecogniser with LazyLogging {
    def onTap(event: MotionEvent)(w: Int, h: Int): 
        (EventRecogniser, Option[InputEvent]) = {
      logger.info("Blank: detected tap")
      this -> Some(InputEvent.Tap(point(event, w, h)))
    }

    def onFling(event1: MotionEvent, event2: MotionEvent)(w: Int, h: Int): 
        (EventRecogniser, Option[InputEvent]) = {
      logger.info("Blank: detected fling")
      val s = point(event1, w, h)
      val e = point(event2, w, h)
      val swipe = InputEvent.DragBecameSwipe(s, List(e))
      this -> Some(swipe)
    }

    def onScroll(event1: MotionEvent, event2: MotionEvent)(w: Int, h: Int): 
        (EventRecogniser, Option[InputEvent]) = {
      logger.info("Blank: detected scroll")
      val s = point(event1, w, h)
      val e = point(event2, w, h)
      Scroll(s, Nil) -> Some(InputEvent.DragStarted(s, e))
    }

    def onActionUp(w: Int, h: Int): (EventRecogniser, Option[InputEvent]) = {
      logger.info("Blank: detected action up")
      this -> None
    }
  }

  case class Scroll(head: InputEvent.Point, tail: List[InputEvent.Point]) 
      extends EventRecogniser with LazyLogging{
    
    def onTap(event: MotionEvent)(w: Int, h: Int): 
        (EventRecogniser, Option[InputEvent]) = {
      logger.info("Scroll: detected tap")
      Blank -> Some(InputEvent.Tap(point(event, w, h)))
    }

    def onFling(event1: MotionEvent, event2: MotionEvent)(w: Int, h: Int): 
        (EventRecogniser, Option[InputEvent]) = {
      logger.info("Scroll: detected fling")
      val end = point(event2, w, h)
      Blank -> Some(InputEvent.DragBecameSwipe(head, tail :+ end))
    }

    def onScroll(event1: MotionEvent, event2: MotionEvent)(w: Int, h: Int): 
        (EventRecogniser, Option[InputEvent]) = {
      logger.info("Scroll: detected scroll")
      val current = point(event2, w, h)
      Scroll(head, tail :+ current) -> 
      Some(InputEvent.DragContinued(head, tail :+ current))
    }

    def onActionUp(w: Int, h: Int): (EventRecogniser, Option[InputEvent]) = {
      logger.info("Scroll: detected scroll finished")
      Blank -> Some(InputEvent.DragFinished(head, tail))
    }
  }
}

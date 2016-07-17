package iliad
package gfx

import iliad.gl._
import iliad.syntax.all._
import iliad.std.list._

import cats._
import cats.data._
import cats.implicits._

import quiver.{LNode, LEdge, Decomp}
import QuiverExtra._

object Output {
  sealed trait Constructor
  sealed trait Instance
}

object Texture {
  sealed trait Uniform
  case class Constructor(name: String,
                         format: gl.Texture.Format,
                         viewport: Vec2i,
                         isDouble: Boolean)
  //TODO: is isDouble something we need to know at this point?
  //Can it be put on the graphConstructor instead, through traversal?
      extends Output.Constructor

  case class Instance(name: String, constructor: Constructor)
      extends Uniform
      with Output.Instance

  case class Image(name: String, format: gl.Texture.Format, viewport: Vec2i)
      extends Uniform
}

object Renderbuffer {
  case class Constructor(name: String,
                         format: RenderbufferInternalFormat,
                         viewport: Vec2i)
      extends Output.Constructor
  case class Instance(name: String, constructor: Constructor)
      extends Output.Instance
}

object Framebuffer {
  sealed trait Constructor
  sealed trait Instance

  case object OnScreen extends Constructor with Instance

  case class OffScreenConstructor(
      buffers: List[(FramebufferAttachment, Output.Constructor)])
      extends Constructor {
    def textures: List[Texture.Constructor] =
      buffers.map(_._2).filterClass[Texture.Constructor]
  }

  case class OffScreenInstance(
      instances: List[(FramebufferAttachment, Output.Instance)])
      extends Instance {

    val hasDoubleTexture: Boolean = instances
      .map(_._2)
      .filterClass[Texture.Instance]
      .exists(_.constructor.isDouble)
  }
}

object Model {
  case class Constructor(name: String)
  case class Instance(name: String, constructor: Constructor, model: gl.Model)
}

sealed trait Node
object Node {
  sealed trait Constructor {
    def name: String
    def framebuffer: Framebuffer.Constructor
    def lNode: LNode[Constructor, String] = LNode(this, name)
  }
  sealed trait Instance {
    def name: String
    def constructor: Constructor
    def lNode: LNode[Instance, String] = LNode(this, name)
  }
  sealed trait Drawable
}

object Draw {
  case class Constructor(
      name: String,
      program: Program.Unlinked,
      primitive: PrimitiveType,
      capabilities: Set[Capability],
      colorMask: ColorMask,
      isInstanced: Boolean,
      model: Model.Constructor,
      framebuffer: Framebuffer.Constructor
  ) extends Node.Constructor

  case class Instance(
      constructor: Constructor,
      uniforms: Map[String, Texture.Uniform],
      model: Model.Instance,
      framebuffer: Framebuffer.Instance,
      numInstances: Int
  ) extends Node.Instance {
    def name: String = toString
    def vertexAttribs: List[Attribute.Constructor] =
      constructor.program.vertex.attributes
    def modelAttribs: List[Attribute.Constructor] =
      model.model.vertex.ref.buffer.attributes
  }

  case class Drawable(
      instance: Instance,
      uniforms: List[Uniform]
  ) extends Node.Drawable
}

object Clear {
  case class Constructor(
      name: String,
      mask: ChannelBitMask,
      framebuffer: Framebuffer.Constructor
  ) extends Node.Constructor

  case class Instance(
      constructor: Constructor,
      framebuffer: Framebuffer.Instance
  ) extends Node.Instance
      with Node.Drawable {
    def name: String = toString
  }
}

sealed trait Link {
  def start: Node.Constructor
  def end: Node.Constructor
  def lEdge: LEdge[Node.Constructor, Link] = LEdge(start, end, this)
}
object Link {

  case class Pipe(start: Draw.Constructor,
                  end: Draw.Constructor,
                  uniforms: Map[String, Texture.Constructor])
      extends Link {
    def textures: Set[Texture.Constructor] = uniforms.values.toSet
    def uniformNames: Set[String] = uniforms.keySet
    def endTextureNames: List[String] = end.program.textureNames
  }

  case class Order(start: Node.Constructor, end: Node.Constructor) extends Link

  case class Instance(start: Node.Instance, end: Node.Instance) {
    def lEdge: LEdge[Node.Instance, Unit] = LEdge(start, end, ())
  }
}

//TODO: find out what to do with this
//case class Valve(start: Node.Draw, links: List[Link.Pipe])

object Graph {
  type Constructor = quiver.Graph[Node.Constructor, String, Link]
  type QInstance = quiver.Graph[Node.Instance, String, Unit]

  val empty: Constructor =
    quiver.empty[Node.Constructor, String, Link]

  case class Constructed(nodes: Set[Node.Constructor],
                         links: Set[Link],
                         start: Set[Node.Constructor],
                         end: Set[Node.Constructor]) {
    def instance: Instance =
      Instance(this, quiver.empty[Node.Instance, String, Unit])
  }

  object Constructed {
    def apply(g: Constructor): Constructed = Constructed(
        g.nodes.toSet,
        g.labEdges.map(_.label).toSet,
        g.roots.toSet,
        g.leaves.toSet
    )
  }

  case class Instance(constructed: Constructed, graph: QInstance) {

    private def addNodes(ns: List[Node.Instance]): State[QInstance, Unit] =
      State.modify(qg => ns.foldLeft(qg)((next, n) => next.addNode(n.lNode)))

    private def addEdges(ls: List[Link.Instance]): State[QInstance, Unit] =
      State.modify(qg => ls.foldLeft(qg)((next, l) => next.addEdge(l.lEdge)))

    def put(ns: List[Node.Instance], ls: List[Link.Instance]): Instance = {
      val next = (addNodes(ns) >> addEdges(ls)).run(graph).value._1
      copy(graph = next)
    }

    def nodes(us: Map[Draw.Instance, List[Uniform]])
      : String Xor Vector[Node.Drawable] =
      graph.ordered.traverse {
          case c: Clear.Instance => c.right
          case d: Draw.Instance =>
            us.get(d)
            .map(Draw.Drawable(d, _))
              .toRightXor(s"Uniforms for node $d do not exist")
      }
  }
}


  abstract class DrawType(val primitive: PrimitiveType)
  object DrawType {
    case object Triangles extends DrawType(GL_TRIANGLES)
    case object Points extends DrawType(GL_POINTS)
  }

  abstract class Dimension(val capabilities: Set[Capability])
  object Dimension {
    case object D2 extends Dimension(Set.empty)
    case object D3 extends Dimension(Set(GL_DEPTH_TEST))
  }
package iliad
package gl

import iliad.syntax.all._
import iliad.std.list._

import cats._
import cats.data._
import cats.implicits._

import quiver.{LNode, LEdge, Decomp}
import QuiverExtra._

object GraphModel {

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
    case class Instance(name: String,
                        constructor: Constructor,
                        model: gl.Model)
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
    ) extends Node.Instance {
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

    case class Order(start: Node.Constructor, end: Node.Constructor)
        extends Link

    case class Instance(start: Node.Instance, end: Node.Instance) {
      def lEdge: LEdge[Node.Instance, Unit] = LEdge(start, end, ())
    }
  }

  //TODO: find out what to do with this
  //case class Valve(start: Node.Draw, links: List[Link.Pipe])

  object Graph {
    type Constructor = quiver.Graph[Node.Constructor, String, Link]
    type QGraph = quiver.Graph[Node.Instance, String, Unit]

    val emptyConstructor: Constructor =
      quiver.empty[Node.Constructor, String, Link]

    case class Constructed(nodes: Set[Node.Constructor],
                           links: Set[Link],
                           start: Set[Node.Constructor],
                           end: Set[Node.Constructor]) {
      def instance: Instance =
        Instance(this, quiver.empty[Node.Instance, String, Unit])
    }

    object Constructed {
      def fromConstructor(g: Constructor): Constructed = Constructed(
          g.nodes.toSet,
          g.labEdges.map(_.label).toSet,
          g.roots.toSet,
          g.leaves.toSet
      )
    }

    case class Instance(constructed: Constructed, graph: QGraph) {

      private def addNodes(ns: List[Node.Instance]): State[QGraph, Unit] =
        State.modify(qg => ns.foldLeft(qg)((next, n) => next.addNode(n.lNode)))

      private def addEdges(ls: List[Link.Instance]): State[QGraph, Unit] =
        State.modify(qg => ls.foldLeft(qg)((next, l) => next.addEdge(l.lEdge)))

      def put(ns: List[Node.Instance], ls: List[Link.Instance]): Instance = {
        val next = (addNodes(ns) >> addEdges(ls)).run(graph).value._1
        copy(graph = next)
      }

      def ordered: Vector[Node.Instance] = graph.ordered
    }
  }
}

/*
object TestGraph {

  val cons = RenderGraph.Constructor.empty

  val vsh = VertexShader.Source("text", List(GLAttribute[Vec2f].attribute("position")), Nil)
  val fsh = FragmentShader.Source("text", List("image" -> Sampler.Constructor.image))
  val program = Program.Unlinked(vsh, fsh)
  val model = RenderGraph.Model.Constructor("model")
  val draw = RenderGraph.Draw.Constructor("basic", program, 
    GL_TRIANGLES,
    Capabilities.depth,
    ColorMask.none,
    false,
    model,
    RenderGraph.Framebuffer.OnScreen
  )
  val instance = cons.put(draw).instance
}
 */
/* We need a state to build up a graph
   We run the state to get the final graph instance
   We validate the instance
   We add a node instance to the graph instance
   We take the graph instance and convert it into a list of draw ops
   We run the draw ops
   We get the load commands out

   Q: Where is the validation?
 **/
/*
object RenderMap {

  /** Switches between multiple links.  All links have the same start node */
  case class Valve(links: List[Link])
}

/** A group of nodes and links */
case class RenderMap(nodes: List[RenderMap.Node],
                     links: List[RenderMap.Link],
                     valves: List[RenderMap.Valve])
 *
 * Any other difficult questions....
 *
 * Q: Once we have a map, how do we add an instance?
 *
 * Any instance must flow from a start node to the end node
 * Certain things are shared across instances
 *
 * Scenario: load and draw the restaurant.
 * The most basic:  We have a single draw drawing untextured vertices to the screen.
 * We add each instance. We need to add vertexData and uniform data only.
 * We can add another instance by adding vertex data
 *
 * More advanced: We want to add shadows to certain objects.
 * For a shadow, we draw all casting objects to the same texture.
 * We then use that texture when drawing all objects to the screen.
 * => we add an instance of a shadow casting object, which means we draw it to the texture
 *
 * Even mode advanced: We want to add lights to certain objects.
 * We draw them with a light source.
 *
 * We want to add bumpmapping to certain objects.
 * We draw them with a normal map.
 *
 * So if we want a shadow casting, bumpmapped, lit object, we go through all the steps.
 *
 * Consider we have multilpe light sources.
 */

/*
trait Loadable {
  def loadCommands[F[_]]: GL.IO[F, Unit]
}

object Basic {

  //this should be 
  val vsh = VertexShader(s"""
#version 300 es

in vec2 position;

void main() {
  gl_Position = vec4(position, 0.0, 1.0);
}

""",
                         List("position" -> ???),
                         Nil,
                         Nil)

  val fsh = FragmentShader(s"""
#version 300 es

out vec4 color;

void main() {
  color = vec4(0.0, 1.0, 0.0, 1.0);
}
""",
                           Nil,
                           Nil,
                           Map.empty)

  val program = Program(vsh, fsh)

  val node = RenderMap.ProgramNode(
      name = "basic",
      program = program,
      primitive = GL_TRIANGLES,
      capabilities = List(GL_DEPTH_TEST),
      isInstanced = false,
      channelBitMask = ChannelBitMask.Empty,
      colorMask = ColorMask(true, true, true, true),
      isOnScreen = true
  )

  //now specific to table
  val bufferInstance = BufferInstance(
      List("position" -> AttributeType[VectorD[nat._2, Float]]),
      GL_TRIANGLES
  )

  val vertexData = Buffer(-1f, -1f, 0f, 1f, 1f, -1f) //size 2 * 3 * 4 = 24
  val elementData = Buffer(0, 1, 2) //size 12

  def loadCommands[F[_]: Monad](gl: GL[F]) =
    for {
      _ <- gl.makeProgram(program)
      _ <- gl.makeModel(
              bufferInstance, GL_ELEMENT_ARRAY_BUFFER, elementData, 12, 2048)
      _ <- gl.makeModel(bufferInstance, GL_ARRAY_BUFFER, vertexData, 24, 2048)
    } yield ()

  //draw commands are part of the node in particular.
  //In a more abstract sense, there is loading the node and loading the model
  //There is drawing the model with the node
  //
  /**
    val instance = node.asGraph.instantiate
    instance.addInstance(node -> tableData)
    -- what does this mean?  tableData consists of a vertexBuffer, elementBuffer reference in a buffer
    -- should be a key e.g. ModelKey
    val tableData = ModelKey("table", vdata, edata)
   load(tableData) andThen instance.addInstance tableData at 30ms
   i.e. load the table data, then add the table instance to the draw at a given time
   we know that the table data has been loaded, because this is done in a stream based system.

   Q: how do we know what instance info to provide?  It would be good to be compile time safe, but not at the moment.
   we assume that we load as soon as we know we are going to draw.
   
   val table = Model("table")
   val chair = Model("chair")

   load(table, chair) andThen addInstance(node -> table) and addInstance(node -> chair)
   //will validate that all info is provided
 */
}

//How do we link buffers? Should we do so?

/**
Imagine we have two nodes, both with the same buffer.
For a given model (table), we want to add it

  addInstance table => {
    node -> table.data1
    node1 -> table.data1, texture1
  }
  so for each node, we specify the data given a model
  we then have all of the instance data.
  and we have a node instance, which contains instances of the textures etc.  
 */

/**
We have a template and an instance
TextureTemplate
RenderbufferTemplate
are used in the graph (instances can be singletons)

when an instance is added, all of the non-singleton variables must be specified

load table andThen 
graph add table {
 bumpMapNode -> Data(table.vertices, table.bumpMapTexture)
 lightDrawNode -> ???
  lightDrawValve -> ???
}
//ERROR: bumpMapNode feeds into lightDrawNode, which is unspecified.
//ERROR: lightDrawNode has valve into draw1, draw2, draw3, which is unspecified
so we start with an entry point, and add things from there

What we have missed:
 - constraining buffers
 - constraining animations
 - pixel draws
 - by default, point buffers are treated like any other
 **/

//constraints can be added in afterwards
 */

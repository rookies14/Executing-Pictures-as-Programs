import com.kitfox.svg.*;
import java.util.Scanner;
import java.io.*;
import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.util.List;
import com.kitfox.svg.animation.AnimationElement;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Line2D;
import java.awt.geom.AffineTransform;
import java.util.Collections;
//JgraphT
import org.jgrapht.*;
import org.jgrapht.ext.*;
import org.jgrapht.graph.*;
import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm.SingleSourcePaths;
import org.jgrapht.alg.interfaces.StrongConnectivityAlgorithm;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.alg.cycle.TarjanSimpleCycles;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;
import org.jgrapht.alg.cycle.CycleDetector;
//JgraphX
import javax.swing.JFrame;
import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;
import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxICell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.util.mxConstants;
import com.mxgraph.layout.*;

//Extractor.java
public class Extractor extends JApplet
{
  private static final Dimension DEFAULT_SIZE = new Dimension(400, 600);
  static ListenableGraph<SVGNode, Arrow> diGraph = new DefaultListenableGraph<>(new DefaultDirectedGraph<SVGNode, Arrow>(Arrow.class));
  static private JGraphXAdapter<SVGNode, Arrow> jgxAdapter;
  static Stack stack = new Stack();
  static ArrayList<SVGNode> nodes = new ArrayList<SVGNode>();
  static Map<String,String> varMap = new HashMap<String,String>();

  public static void main(String[] args)
  {
    Extractor applet = new Extractor();
    jgxAdapter = new JGraphXAdapter<>(diGraph);
    applet.init();
  
    char cChecker = '1';
    URI uri = null;
    try {
      File f = new File(args[0]);
      uri = f.toURI();
    } 
    catch (Exception e) {
      System.out.println(e);
    }
    SVGUniverse svgUniverse = new SVGUniverse();
    SVGDiagram diagram = svgUniverse.getDiagram(uri);
    int objCount = 0;
    for(int i=0;i<diagram.getRoot().getNumChildren();i++){
      SVGElement elem =diagram.getRoot().getChild(i);
      objCount = findElem(elem,objCount);
    }
    printNode();
    compareNode(objCount);   

    // changes value of shape from name into text label
    mxCell cell = (mxCell)jgxAdapter.getDefaultParent();
    for(int i=0;i<cell.getChildCount();i++){
      mxICell icell = cell.getChildAt(i);
      if(icell.getValue() instanceof Rect){
        String strTemp = ((Rect)icell.getValue()).text;
        String type = ((Rect)icell.getValue()).type;
        icell.setValue(((Rect)icell.getValue()).text);
        icell.setGeometry(new mxGeometry(0,0,strTemp.length()*5+30,20));
        Map<String, Object> styles = 
         new HashMap<String, Object>(jgxAdapter.getView().getState(icell).getStyle());
         // configs styles of JGraphX with mxConstants class which contains styles of mxGraph
        styles.put(mxConstants.STYLE_FONTCOLOR, "black");
        if(type.equals("process"))
          styles.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_RECTANGLE);
        else if(type.equals("condition"))
          styles.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_RHOMBUS);
        else if(type.equals("terminal"))
          styles.put(mxConstants.STYLE_ROUNDED, "1");
        jgxAdapter.getView().getState(icell).setStyle(styles);
        icell.setVisible(true);
      }
      if(icell.getValue() instanceof Circle){
        icell.setValue(null);
        icell.setGeometry(new mxGeometry(0,0,20,20));
        Map<String, Object> styles = 
         new HashMap<String, Object>(jgxAdapter.getView().getState(icell).getStyle());
        styles.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_ELLIPSE);
        jgxAdapter.getView().getState(icell).setStyle(styles);
        icell.setVisible(true);
      }

      // changes edge value as true or false if previous is diamond shape
      if(icell.getValue() instanceof Arrow)
        icell.setValue(((Arrow)icell.getValue()).condition);
    }

    // transforms graph layout as Hierarchical style
    mxHierarchicalLayout layout = new mxHierarchicalLayout(jgxAdapter);;
    layout.execute(jgxAdapter.getDefaultParent());

    // genarates JApplet
    JFrame frame = new JFrame();
    frame.getContentPane().add(applet);
    frame.setTitle("JGraphT Adapter to JGraphX");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.pack();
    frame.setVisible(true);

    printJGraphT();

    while(!stack.empty())
      System.out.println(stack.pop());
  }
  public void init()
  {
    //JGrapX initialize
    jgxAdapter = new JGraphXAdapter<>(diGraph);
    setPreferredSize(DEFAULT_SIZE);
    mxGraphComponent component = new mxGraphComponent(jgxAdapter);
    component.setConnectable(false);
    component.getGraph().setAllowDanglingEdges(false);
    getContentPane().add(component);
    resize(DEFAULT_SIZE);
  }
  static public void printJGraphT()
  {
    // prints all cycles
    TarjanSimpleCycles tsCycles = new TarjanSimpleCycles<SVGNode, Arrow> (diGraph);
    for (int i = 0; i < tsCycles.findSimpleCycles().size(); i++) {
      System.out.println("\nCycles:");
      List<Rect> s = (List<Rect>)tsCycles.findSimpleCycles().get(i);
      for(int j = 0; j < s.size(); j++){
        if(s instanceof Rect){
          Rect z = (Rect)s.get(j);
        }
      }
    }

    // find start point and target point of the graph
    SVGNode start = null;
    SVGNode dest = null;
    for(SVGNode proc : nodes){
      if(proc instanceof Rect){
        if(((Rect)proc).text.equalsIgnoreCase("start"))
          start = (Rect)proc;       
        if(((Rect)proc).text.equalsIgnoreCase("end"))
          dest = (Rect)proc;
      }
    }

    // prints all paths
    AllDirectedPaths<SVGNode, Arrow> allPaths = new AllDirectedPaths<> (diGraph);
    List<GraphPath<SVGNode, Arrow>> allPathsList = allPaths.getAllPaths(start,dest,true,20);
    System.out.println("\nAll paths from Start to End:");
    for(GraphPath p : allPathsList) {
      System.out.println("\nPath###");
      SVGNode source,target = null;
      List<Arrow> allPathsEdgeList = p.getEdgeList();
      for(Arrow pl : allPathsEdgeList){
        source = (SVGNode)p.getGraph().getEdgeSource(pl);
        target = (SVGNode)p.getGraph().getEdgeTarget(pl);
        if(source instanceof Rect)
          System.out.println("\t->"+((Rect)source).text+" ("+source.type+") "+pl.condition);
      }
      if(target instanceof Rect)
        System.out.println("\t->"+((Rect)target).text+" ("+target.type+")");
    }

    // prints the shortest path
    DijkstraShortestPath<SVGNode, Arrow> dsPath = new DijkstraShortestPath<>(diGraph);
    GraphPath<SVGNode, Arrow> shortestPath = dsPath.getPath(start,dest);
    System.out.println("\nShortest path from Start to End:");
      SVGNode source,target = null;
      List<Arrow> allPathsEdgeList = shortestPath.getEdgeList();
      for(Arrow pl : allPathsEdgeList){
        source = (SVGNode)shortestPath.getGraph().getEdgeSource(pl);
        target = (SVGNode)shortestPath.getGraph().getEdgeTarget(pl);
        if(source instanceof Rect)
          System.out.println("\t->"+((Rect)source).text+" ("+source.type+") "+pl.condition);
      }
      if(target instanceof Rect)
        System.out.println("\t->"+((Rect)target).text+" ("+target.type+")");
    
    String result = convertDiagram(shortestPath.getStartVertex());
    System.out.println(result);
  }

  public static void declareVar(String body,String var)
  //add new variable by put variables into varMap
  {
    //deals when user declare multiple variables at once
    if(body.contains(",")){
      String[] strArray = body.split(","); 
      for(String str : strArray){
        //deal with variable which initialized with value
        if(str.contains("="))
          str = str.split("=")[0];
        //put variable and its type in varMap
        varMap.put(str,var);
      }
    }
    else {
      if(body.contains("="))
          body = body.split("=")[0];
      varMap.put(body,var);
    }
  }

  public static String makeCode(String body,String sps)
  //converts text to code
  {
    if(body.contains("Start"))  
      return "main(){";
    else if (body.contains("End"))
      return "}";
    else if (body.contains("Set")){
      body = body.replaceAll("Set ","");
      return body;
    }
    else if (body.contains("Declare")){
      if(body.contains("Integer")){
        body = body.replaceAll("Declare Integer ","");

        //calls declareVar to put variable into variable map
        declareVar(body,"integer");
        return "int "+body+";";
      } 
      if(body.contains("Unsigned Integer")){
        body = body.replaceAll("Declare Unsigned Integer ","");
        declareVar(body,"unsigned integer");
        return "unsigned int "+body+";";
      } 
      else if(body.contains("String")){
        body = body.replaceAll("Declare String ","");
        declareVar(body,"string");
        return "string "+body+";";
      }
      else if(body.contains("Float")){
        body = body.replaceAll("Declare Float ","");
        declareVar(body,"float");
        return "float "+body+";";
      }
      else if(body.contains("Char")){
        body = body.replaceAll("Declare Char ","");
        declareVar(body,"char");
        return "char "+body+";";
      }
      else if(body.contains("Unsigned Char")){
        body = body.replaceAll("Declare Unsigned Char ","");
        declareVar(body,"unsigned char");
        return "unsigned char "+body+";";
      }
      else if(body.contains("Short")){
        body = body.replaceAll("Declare Short ","");
        declareVar(body,"short");
        return "short"+body+";";
      }
      else if(body.contains("Long")){
        body = body.replaceAll("Declare Long ","");
        declareVar(body,"long");
        return "long"+body+";";
      }
      else if(body.contains("Unsigned Long")){
        body = body.replaceAll("Declare Long ","");
        declareVar(body,"unsigned long");
        return "unsigned long"+body+";";
      }
      else if(body.contains("Double")){
        body = body.replaceAll("Declare Double ","");
        declareVar(body,"double");
        return "double"+body+";";
      }
      else if(body.contains("Long Double")){
        body = body.replaceAll("Declare Long Double ","");
        declareVar(body,"long double");
        return "long double"+body+";";
      }
    }
    else if (body.contains("Input")){
      body = body.replaceAll("Input ","");

      //if user didn't declare variable the program will use scanf without data format
      if(varMap.get(body)==null)
        return "scanf("+body+");";
      String dataFormat = getDataFormat(varMap.get(body));
      return "scanf(\""+dataFormat+"\",&"+body+");";
    }
    else if (body.contains("Print")){
      body = body.replaceAll("Print ","");
      StringBuilder strCode = new StringBuilder();

      //in case the text has "+" it will split the text to multiple lines of code
      if(body.contains("+")){
        String[] strArray = body.split("\\+"); 
        for(String str : strArray){
          if(strCode.length()!=0)
            strCode.append("\n"+sps+makePrintf(str));
          else
            strCode.append(makePrintf(str));
        }
      } 
      else
        strCode.append(makePrintf(body));
      return strCode.toString();
    }
    return body+";";
  }

  public static String makePrintf(String body)
  //seperated print part from makeCode function
  {
    if(body.contains("\"")){
      body = body.replaceAll("\"","");
      return "printf(\""+body+"\");";
    } 
    else {
      //if user didn't declare variable the program will use printf without data format
      if(varMap.get(body)==null)
        return "printf("+body+");";
      String dataFormat = getDataFormat(varMap.get(body));
      return "printf(\""+dataFormat+"\","+body+");";
    }
  }

  public static String getDataFormat(String data)
  //gets data format compare with input string
  {
    if(data.equals("string"))
      return "%s";
    else if(data.equals("integer"))
      return "%d";
    else if(data.equals("unsigned integer"))
      return "%u";
    else if(data.equals("float"))
      return "%f";
    else if(data.equals("char"))
      return "%c";
    else if(data.equals("unsigned char"))
      return "%c";
    else if(data.equals("short"))
      return "%d";
    else if(data.equals("long"))
      return "%ld";
    else if(data.equals("unsigned long"))
      return "%lu";
    else if(data.equals("double"))
      return "%lf";
    else if(data.equals("unsigned double"))
      return "%Lf";
    else
      return "";
  }

  public static Graph getPathToTarget(SVGNode source,SVGNode target)
  //gets and returns path between starting node to the target as a graph
  {
    //if the target node is null it will gets the path to "end" node
    HashSet pathSet = new HashSet();
    if(target==null){
      for(SVGNode proc : nodes){
        if(proc instanceof Rect){   
          if(((Rect)proc).text.equalsIgnoreCase("end"))
            target = (Rect)proc;
        }
      }
    }

    //gets shortest path between source node and target node
    DijkstraShortestPath<SVGNode, Arrow> dsPath = new DijkstraShortestPath<>(diGraph);
    GraphPath<SVGNode, Arrow> shortestPath = dsPath.getPath(source,target);
    for(SVGNode proc : shortestPath.getVertexList())
      pathSet.add(proc);
    return new AsSubgraph<SVGNode, Arrow>(diGraph, pathSet);
  }

  public static Graph getLoopGraph(SVGNode start)
  {
    HashSet whileSet = new HashSet();
    List<SVGNode> loopList = null;
    //finds all loops from TarjanSimpleCycles then check with starting vertex
    TarjanSimpleCycles<SVGNode, Arrow> tjCycles = new TarjanSimpleCycles<SVGNode, Arrow>(diGraph);
    List<List<SVGNode>> allCycles = tjCycles.findSimpleCycles();
    //find a cycle which match with start node
    for(List<SVGNode> list : allCycles){
      if(!(list.get(0).isShape("condition")))
        Collections.reverse(list); 
      if(start==list.get(0)){
        loopList = list;
        break;
      }
    }
    //creates new subgraph which contain this cycle
    if(loopList!=null){
      for(SVGNode proc : loopList){
        if(proc instanceof Rect)
        whileSet.add(proc);
      }
      return new AsSubgraph<SVGNode,Arrow>(diGraph, whileSet,diGraph.edgeSet());
    }
    return null;
  }

  public static class ConvertInfo
  //this class made to allow any class to return 2 values at once
  {
    //text is needed for convertDiamond to return loop/if-else string
    String text;

    //nextNode is needed for convertDiamond to return next node to process
    SVGNode nextNode;
    public ConvertInfo(String text,SVGNode nextNode)
    {
      this.text = text;
      this.nextNode = nextNode;
    }
  }

  public static SVGNode findExitCircle(Graph tPaths,Graph fPaths,SVGNode start)
  //finds both paths and returns a node where both paths meet each other
  {
    Set<SVGNode> trueNodeSet = tPaths.vertexSet();
    List<SVGNode> tList = new ArrayList(trueNodeSet);
    Collections.reverse(tList);
    Set<SVGNode> falseNodeSet = fPaths.vertexSet();
    for(SVGNode node1 : tList){
      for(SVGNode node2 : falseNodeSet){
        if((node1==node2)&&(node1.type.equals("circle"))){
          return node1;
        }
      }
    }
    return null;
  }

  public static boolean isLoop(SVGNode node)
  {
    /*This can prevert errors in case if-else inside while
      While loop always has 2+ incoming edges
    */
    Set<Arrow> edgesSet = diGraph.incomingEdgesOf(node);
    List<Arrow> edgesList = new ArrayList<>(edgesSet);
    if(edgesSet.size()>1)
      return true;
    else
      return false;
  }

  public static ConvertInfo convertIf(SVGNode start,String sps)
  {
    //gets first node of both side of current node
    SVGNode trueBranch = getBranch(start,true);
    SVGNode falseBranch = getBranch(start,false);
    /*gets true and false paths to the end of diagram
      if there are more paths then calling another convertDiamond
      will deal with more paths
    */
    Graph truePaths = getPathToTarget(trueBranch,null);
    Graph falsePaths = getPathToTarget(falseBranch,null);

    //finds and gets the same circle from both side of if and else
    SVGNode nextNode = findExitCircle(truePaths,falsePaths,start);

    //gets paths to that circle
    truePaths = getPathToTarget(trueBranch,nextNode);
    falsePaths = getPathToTarget(falseBranch,nextNode);
    String ifString = sps+"if("+((Rect)start).text+"){\n"+convertGraph(truePaths,trueBranch,sps+"  ",false)+"\n"+sps+"}\n"+sps+"else{\n"+convertGraph(falsePaths,falseBranch,sps+"  ",false)+"\n"+sps+"}";
    return new ConvertInfo(ifString,nextNode); 
  }

  public static ConvertInfo convertLoop(SVGNode start,String sps)
  {
    //gets first node of both side of current node
    SVGNode trueBranch = getBranch(start,true);
    SVGNode falseBranch = getBranch(start,false);
    SVGNode nextNode = null;
    String loopString="";

    //CycleDetector checks loops in the graph
    CycleDetector cDetector = new CycleDetector(diGraph);

    //checks if there is a loop in any side of the condition
    if(cDetector.detectCyclesContainingVertex(trueBranch)){
      Graph trueLoopBranch = getLoopGraph(start);

      //converts graph to code by calling convertGraph method
      loopString = sps+"while("+((Rect)start).text+"){\n"+convertGraph(trueLoopBranch,start,sps+"  ",true)+"\n"+sps+"}";
      nextNode = falseBranch;
    }
    else if(cDetector.detectCyclesContainingVertex(falseBranch)){
      Graph falseLoopBranch = getLoopGraph(start);
      loopString = sps+"while(!"+((Rect)start).text+"){\n"+convertGraph(falseLoopBranch,start,sps+"  ",true)+"\n"+sps+"}";
      nextNode = trueBranch;
    }
    return new ConvertInfo(loopString,nextNode); 
  }

  public static ConvertInfo convertDiamond(SVGNode start,String sps)
  //converts loop or if-else graph then return text and next node
  {
    ConvertInfo info = null;
    if(isLoop(start))
      info = convertLoop(start,sps);
    else
      info = convertIf(start,sps);
    return info;
  }

  public static String convertGraph(Graph graph,SVGNode start,String sps,boolean isLoop)
  {
    StringBuilder codeStr = new StringBuilder();
    
    //returns nothing if graph is null
    if(graph==null)
      return "";
    Set<SVGNode> vertexSet = graph.vertexSet();
    List<SVGNode> vertexList = new ArrayList(vertexSet);

    //finds start node from new graph not global graph
    SVGNode node=null;
    for(SVGNode proc : vertexList){
      if(proc==start)
        node=proc;
    }

    /*getting node from by vertexSet will not sorted correctly
      so using nextVertex from current graph is better
    */
    if(isLoop)
      node=nextVertex(node,graph);
    while(node!=null){
      /*if current vertex is the start vertex it will break or if 
        current vertex is another condition it will call convertDiamond
      */
      if(node.isShape("condition")){
        if(node==start&&isLoop)
          break;
        ConvertInfo info = convertDiamond(node,sps);
        if(codeStr.length()!=0)
          codeStr.append("\n"+info.text);
        else
          codeStr.append(info.text);
        node=info.nextNode;
      }
      //if current vertex is one of Rect class it will append text
      else if(node instanceof Rect){
        if(codeStr.length()!=0)
          codeStr.append("\n"+sps+makeCode(((Rect)node).text,sps));
        else
          codeStr.append(sps+makeCode(((Rect)node).text,sps));
        node=nextVertex(node,graph);
      }
      //deal with other classes which have no text inside
      else {
        if(!isLoop){
          
          break;
        }
        else{
          
          node=nextVertex(node,diGraph);
          if(node!=null)
          System.out.println(((Rect)node).text);
        }
      }
      
    }
    return codeStr.toString();
  }

  public static String convertDiagram(SVGNode vertex)
  //main function to convert each node of the diagram
  {
    StringBuilder codeStr = new StringBuilder();
    while(vertex!=null){
      if(vertex.isShape("condition")){
        ConvertInfo info = convertDiamond(vertex,"");
        codeStr.append("\n"+info.text);
        vertex = info.nextNode;
      }

      /*if it is not condition it will add code
        Rect included with condition,terminal,process,data
      */
      else if(vertex instanceof Rect){
        codeStr.append("\n"+makeCode(((Rect)vertex).text,""));  
        vertex = nextVertex(vertex,diGraph);
      } 
      else 
        vertex = nextVertex(vertex,diGraph);
    }
    return codeStr.toString();
  }
  
  public static SVGNode getBranch(SVGNode vertex,boolean edge)
  {
    //gets branch of any true or false of vertex
    Set<Arrow> edgesSet = diGraph.outgoingEdgesOf(vertex);
    List<Arrow> edgesList = new ArrayList<>(edgesSet);
    for(Arrow arrow : edgesList){
      if(arrow.condition.equalsIgnoreCase("true")&&(edge))
        return diGraph.getEdgeTarget(arrow);
      else if(arrow.condition.equalsIgnoreCase("false")&&(!edge))
        return diGraph.getEdgeTarget(arrow);
    }
    return null;
  }
  
  public static SVGNode nextVertex(SVGNode vertex,Graph graph)
  {
    //gets next vertex of current vertex
    if(vertex==null)
      return null;
    Set<Arrow> edgesSet = null;
    try{
      edgesSet = graph.outgoingEdgesOf(vertex);
    } catch(Exception e){
      return null;
    }
    List<Arrow> edgesList = new ArrayList<>(edgesSet);
    if(edgesList.size()!=0)
      return (SVGNode)graph.getEdgeTarget(edgesList.get(0));
    return null;
  }

  public static float getPresValue(SVGElement element,String att)
  {
    return Float.parseFloat(element.getPresAbsolute(att).getStringValue());
  }

  public static boolean containsIgnoreCase(String str1,String str2)
  {
    if(str1.toLowerCase().contains(str2.toLowerCase()))
      return true;
    else 
      return false; 
  }

  public static void compareNode(int objCount)
  {
    for(SVGNode proc : nodes){
      if(proc instanceof Arrow){
        Arrow arrow = (Arrow)proc;
        SVGNode connection1 = printConnect(arrow.point.getP1(),"Arrow");
        SVGNode connection2 = printConnect(arrow.point.getP2(),"Arrow");
        if(connection1!=null&&connection2!=null){
          diGraph.addVertex(connection1);
          diGraph.addVertex(connection2);
          diGraph.addEdge(connection1,connection2,arrow);
        }
      } 
    }

    for(SVGNode proc : nodes){
      if(proc instanceof Text){
        Text text = (Text)proc;
        if(containsIgnoreCase(text.lbText,"true")||containsIgnoreCase(text.lbText,"false")){
          SVGNode textTemp = printConnect(text.point,text.lbText);
          ((Arrow)textTemp).condition = text.lbText;
        }
      }
    }
  }
  public static SVGNode printConnect(Point2D point,String checkType){
    String txt = "";
    double distanceRect=1000,distanceArrow=1000;
    SVGNode nearestRect = null;
    Arrow nearestArrow = null;
    for(SVGNode proc : nodes){
      if(proc instanceof Rect){
        Point2D.Float centerPoint = new Point2D.Float((float)((Rect)proc).rect.getCenterX(),(float)((Rect)proc).rect.getCenterY());
        if(point.distance(centerPoint)<distanceRect){
          nearestRect = proc;
          distanceRect = point.distance(centerPoint);
        }
      } 
      if(proc instanceof Circle){
        if(point.distance(((Circle)proc).point)<distanceRect){
          nearestRect = proc;
          distanceRect = point.distance(((Circle)proc).point);
        }
      }
      if(proc instanceof Arrow){
        if(((Arrow)proc).point.ptSegDist(point)<distanceArrow){
          nearestArrow = ((Arrow)proc);
          distanceArrow = ((Arrow)proc).point.ptSegDist(point);
        }
      }
    }
    if(checkType.equalsIgnoreCase("true")||checkType.equalsIgnoreCase("false"))
      return nearestArrow;
    for(SVGNode temp : nodes){
      if(temp instanceof Text){
        Text text = (Text)temp;
        if(nearestRect instanceof Rect){
          if(((Rect)nearestRect).checkConnect(text.point)){
            ((Rect)nearestRect).text = text.lbText;
            txt = text.lbText;
          }
        } 
      }
    }
    return nearestRect;
  }
  public static void printNode(){
    for(SVGNode proc : nodes){
      if(proc instanceof Rect)
        ((Rect)proc).printAll();
      else if(proc instanceof Text)
        ((Text)proc).printAll();
      else if(proc instanceof Arrow)
        ((Arrow)proc).printAll();
      else if(proc instanceof Circle){
        ((Circle)proc).printAll();
      }   
    }
  }
  public static int findElem(SVGElement elem,int objCount){
    objCount++;
    for(int i = 0; i < elem.getNumChildren(); i++) {
      storeNode(elem,objCount,i);
      SVGElement element  = elem.getChild(i);
      objCount = findElem(element, objCount);
    }
    return objCount;
  }
  public static void storeNode(SVGElement elem,int i,int objCount){
    float[] tmpx = new float[4];
    float[] tmpy = new float[4];
    String tText = "";
    String type = "";
    String att[] = {"x","y","width","height","rx","ry","cx","cy","transform","xml:space","sodipodi:role"};
    AffineTransform matrix = new AffineTransform(0,0,0,0,0,0);
    SVGElement element = elem.getChild(objCount);   
    try{
      for(int j=0;j<att.length;j++){
        if(element.hasAttribute(att[j],AnimationElement.AT_AUTO)){
          if(att[j]=="x"){
            tmpx[0] = getPresValue(element,att[j]);
            type = "process";
          }
          if(att[j]=="y"){
            tmpy[0] = getPresValue(element,att[j]);
            type = "process";
          }
          if(att[j]=="width")
            tmpx[1] = getPresValue(element,att[j]);          
          if(att[j]=="height")
            tmpy[1] = getPresValue(element,att[j]);
          if(att[j]=="rx"){
            tmpx[2] = getPresValue(element,att[j]);
            type = "terminal";
          }
          if(att[j]=="ry"){
            tmpy[2] = getPresValue(element,att[j]);
            type = "terminal";
          }
          if(att[j]=="cx"){
            tmpx[0] = getPresValue(element,att[j]);
            type= "circle";
          }
          if(att[j]=="cy"){
            tmpy[0] = getPresValue(element,att[j]);
            type = "circle";
          }
          if(att[j]=="transform"){
            String trans = element.getPresAbsolute(att[j]).getStringValue();
            matrix = element.parseSingleTransform(trans);
            type = "data";
          }
          if(att[j]=="xml:space"){
            for(int tcount=0;tcount<element.getNumChildren();tcount++) {
              Tspan text = (Tspan) (element.getChild(tcount));
              tText = tText + text.getText();
            }
            type = "text";
          }
          if(att[j]=="sodipodi:role")
            type = "tspan";     
        }
      }
    }
    catch(SVGException e) { System.out.println(e);  }
    try{
      if(element.hasAttribute("d",AnimationElement.AT_AUTO)){
        String[] info=element.getPresAbsolute("d").getStringValue().split(" ");
        for(int z=0; z<info.length;z++){
          if(!(info[0].contains("M")||info[z].contains("m")))
            type = "circle";       
          if(info[z].contains("M")||info[z].contains("m")){
            if(z!=0){
              String s[] = info[z-1].split(",");
              if(info[0].contains("m")){
                tmpx[0] = Float.parseFloat(s[0])+tmpx[1];
                tmpy[0] = Float.parseFloat(s[1])+tmpy[1];
              } 
              else {
                tmpx[0] = Float.parseFloat(s[0]);
                tmpy[0] = Float.parseFloat(s[1]);
              }
              type = "arrow";
              break;
            }
            String s[] = info[z+1].split(",");
            tmpx[1] = Float.parseFloat(s[0]);
            tmpy[1] = Float.parseFloat(s[1]);           
            z++;                      
          }
          else if(info[z].contains("V")||info[z].contains("v")){
            tmpx[0] = tmpx[1];
            if(info[z].contains("V"))
              tmpy[0] = Float.parseFloat(info[z+1]);           
            if(info[z].contains("v"))
              tmpy[0] = tmpy[1]+Float.parseFloat(info[z+1]);           
            type = "arrow";                    
            break;
          }
          else if(info[z].contains("C")||info[z].contains("c"))
            z+=2;
          else if(info[z].contains("H"))
            System.out.println("\tX-Destination point : -"+info[z+1]);
          else if(info[z].contains("h"))
            System.out.println("\tX-Destination point : "+info[z+1]);
          else if(info[z].contains("l")||info[z].contains("L")){
            System.out.println("\tCondition point : "+info[z+1]);
            type = "condition";
          } 
          else if(info[z].contains("Z")||info[z].contains("z")){
            for(int j=1;j<5;j++){
                String s[] = info[j].split(",");
                tmpx[j-1] = Float.parseFloat(s[0]);
                tmpy[j-1] = Float.parseFloat(s[1]);
            }
            type = "condition";
            break;
          }
        }
      }
      else if(!element.hasAttribute("d",AnimationElement.AT_AUTO))
      {
        if(element.getId().contains("path"))
          type="circle";       
      }
    }
    catch(SVGException e) { System.out.println(e);  }
    tmpx[0] = (float)((int)( tmpx[0] *100f))/100f;
    tmpy[0] = (float)((int)( tmpy[0] *100f))/100f;
    tmpx[1] = (float)((int)( tmpx[1] *100f))/100f;
    tmpy[1] = (float)((int)( tmpy[1] *100f))/100f;
    tmpx[2] = (float)((int)( tmpx[2] *100f))/100f;
    tmpy[2] = (float)((int)( tmpy[2] *100f))/100f;
    tmpx[3] = (float)((int)( tmpx[3] *100f))/100f;
    tmpy[3] = (float)((int)( tmpy[3] *100f))/100f;
    if(type == "circle"){
      Point2D.Float tempPoint = new Point2D.Float(tmpx[0],tmpy[0]);
      SVGNode temp = new Circle(element.getId(),"circle",tmpx[0],tmpy[0]); 
      nodes.add(temp);
    } 
    else if(type == "process"){
      Rectangle2D.Float tempRect = new Rectangle2D.Float(tmpx[0],tmpy[0],tmpx[1],tmpy[1]);
      SVGNode temp = new Rect(element.getId(),"process",tempRect);
      nodes.add(temp);
    } 
    else if (type == "terminal"){
      Rectangle2D.Float tempRect = new Rectangle2D.Float(tmpx[0],tmpy[0],tmpx[1],tmpy[1]);
      SVGNode temp = new Rect(element.getId(),"terminal",tempRect);
      nodes.add(temp);
    } 
    else if (type == "data"){
      Rectangle2D.Float tempRect = new Rectangle2D.Float(tmpx[0],tmpy[0],tmpx[1],tmpy[1]);
      Rectangle2D temp2D = matrix.createTransformedShape(tempRect).getBounds2D();
      tempRect = new Rectangle2D.Float((float)temp2D.getX(),(float)temp2D.getY(),(float)temp2D.getWidth(),(float)temp2D.getHeight());
      SVGNode temp = new Rect(element.getId(),"data",tempRect);
      nodes.add(temp);
    } 
    else if (type == "condition"){
      Rectangle2D.Float tempRect = null;
      if(tmpy[1]==tmpy[2])
        tempRect = new Rectangle2D.Float(tmpx[0]-tmpx[1],tmpy[0],tmpx[1]*2,tmpy[1]*2);
      else {
        tempRect = new Rectangle2D.Float(tmpx[0],tmpy[0],-1,-1);
        for (int k = 0; k < 4; k++) {
          tempRect.add(tmpx[k],tmpy[k]);
        }
      }
      SVGNode temp = new Rect(element.getId(),"condition",tempRect);
      nodes.add(temp);
    } 
    else if (type == "arrow"){
      SVGNode temp = new Arrow(element.getId(),"arrow",tmpx[0],tmpy[0],tmpx[1],tmpy[1]);
      nodes.add(temp);
    } 
    else if (type == "text"){
      Point2D.Float tempPoint = new Point2D.Float(tmpx[0],tmpy[0]);
      SVGNode temp = null;
      if((float)matrix.getScaleY()!=0)
        temp = new Text(element.getId(),"text",tText,tmpx[0],tmpy[0]*(float)matrix.getScaleY());
      else
        temp = new Text(element.getId(),"text",tText,tmpx[0],tmpy[0]); 
      nodes.add(temp);
    }

  }
  public static class SVGNode
  {
    String id;
    String type;
    public SVGNode(String id,String type)
    {
      this.id = id;
      this.type = type;
    }
    public boolean isShape(String shape)
    {
      if(type.contains(shape))
        return true;
      return false;
    }
  }
  public static class Circle extends SVGNode
  {
    Point2D.Float point;
    public Circle(String id,String type,float iX,float iY)
    {
      super(id,type);
      this.point = new Point2D.Float(iX,iY);
    }
    public void printAll()
    {
      System.out.println("\nID :"+id);
      System.out.println("\ttype :"+this.getClass().getSimpleName());
      System.out.println("\tPosition(X,Y) : "+point.toString());
    }
  }
  public static class Rect extends SVGNode
  {
    Rectangle2D.Float rect;
    String text="";
    public Rect(String id,String type,Rectangle2D.Float rect)
    {
      super(id,type);
      this.rect=rect;
    }
    public void printAll()
    {
      System.out.println("\nID :"+id);
      System.out.println("\ttype :"+type);
      System.out.println("\tPosition(X,Y) : "+rect.toString());
    }
    public boolean checkConnect(Point2D dest)
    {
      return rect.contains(dest);
    }
  }
  public static class Arrow extends SVGNode implements Serializable, Cloneable
   {
    Line2D.Float point;
    String condition="";
    public Arrow(String id,String type,float startX,float startY,float destX,float destY)
    {
      super(id,type);
      startX = (float)((int)( startX *100f))/100f;
      startY = (float)((int)( startY *100f))/100f;
      destX = (float)((int)( destX *100f))/100f;
      destY = (float)((int)( destY *100f))/100f;
      this.point = new Line2D.Float(startX,startY,destX,destY);
    }
    public void printAll()
    {
      System.out.println("\nID :"+id);
      System.out.println("\ttype :"+this.getClass().getSimpleName());
      System.out.println("\tStarting point : "+point.getP1());
      System.out.println("\tDestination point : "+point.getP2());
    }
  }
  public static class Text extends SVGNode
  {
    String lbText;
    Point2D.Float point;
    public Text(String id,String type,String lbText,float iX,float iY)
    {
      super(id,type);
      this.lbText = lbText;
      this.point = new Point2D.Float(iX,iY);
    }
    public void printAll()
    {
      System.out.println("\nID :"+id);
      System.out.println("\ttype :"+this.getClass().getSimpleName());
      System.out.println("\tPosition(X,Y) : "+point.toString());
      System.out.println("\tText : "+lbText);  
    }
  }
}

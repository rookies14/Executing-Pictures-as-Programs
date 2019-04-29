import com.kitfox.svg.***;
import java.util.Scanner;
import java.io.*;
import java.net.URI;
import com.kitfox.svg.animation.AnimationElement;
import java.util.ArrayList;

//NSExtractor.java
public class NSExtractor
{
  static ArrayList<SVGNode> nodes = new ArrayList<SVGNode>();
  public static void main(String[] args){
    
    char cChecker = '1';
    URI uri = null;
    try {
      File f = new File(args[0]);
      uri = f.toURI();
    } 
    catch (Exception e)
      System.out.println(e);
    
    SVGUniverse svgUniverse = new SVGUniverse();
    SVGDiagram diagram = svgUniverse.getDiagram(uri);
    

    int objCount = 0;
    for(int i = 0; i < diagram.getRoot().getNumChildren(); i++) {
      
      SVGElement elem  = diagram.getRoot().getChild(i);
      objCount = findElem(elem,objCount);
    }
    printNode();
  }
  public static void printNode()
  {
    for(SVGNode proc : nodes){
      if(proc instanceof Rectangle){
        Rectangle aaa = (Rectangle)proc;
        aaa.printAll();
      } 
      else if(proc instanceof Triangle){
        Triangle aaa = (Triangle)proc;
        aaa.printAll();
      } 
      else if(proc instanceof Text){
        Text aaa = (Text)proc;
        aaa.printAll();
      }
    }
  }
  public static int findElem(SVGElement elem,int objCount)
  {
    objCount++;
    for(int i = 0; i < elem.getNumChildren(); i++) {
      storeNode(elem,objCount,i);
      SVGElement element  = elem.getChild(i);
      objCount = findElem(element, objCount); 
    }
    return objCount;
  }
  public static void storeNode(SVGElement elem,int i,int objCount)
  {
    float[] tmpx = new float[3];
    float[] tmpy = new float[3];
    String tText = "";
    String sType = "";
    String pres[]= {"x","y","width","height","cy","cx", "rx", "ry"};
    SVGElement element = elem.getChild(objCount);
    
    try{
      if(element.hasAttribute("x",AnimationElement.AT_AUTO)){
        tmpx[0] = Float.parseFloat(element.getPresAbsolute("x").getStringValue());
        sType = "rectangle";
      }
      if(element.hasAttribute("y",AnimationElement.AT_AUTO)){
        tmpy[0] = Float.parseFloat(element.getPresAbsolute("y").getStringValue());
        sType = "rectangle";
      }
      if(element.hasAttribute("width",AnimationElement.AT_AUTO))
        tmpx[1] = Float.parseFloat(element.getPresAbsolute("width").getStringValue());
      if(element.hasAttribute("height",AnimationElement.AT_AUTO))
        tmpy[1] = Float.parseFloat(element.getPresAbsolute("height").getStringValue());
      if(element.hasAttribute("xml:space",AnimationElement.AT_AUTO)){
        Tspan text = (Tspan) (element.getChild(0));
        tText = text.getText();
        sType = "text";
      }
      if(element.hasAttribute("sodipodi:role",AnimationElement.AT_AUTO))
        sType = "tspan";
    }
    catch(SVGException e)
      System.out.println(e);
    
    try{
      if(element.hasAttribute("d",AnimationElement.AT_AUTO)){
        String[] info = element.getPresAbsolute("d").getStringValue().split(" ");
        for(int z=0; z<info.length;z++){
          if(info[z].contains("M")||info[z].contains("m")){
            if(z!=0){
              String s[] = info[z-1].split(",");
              if(info[0].contains("m")){
                tmpx[0] = Float.parseFloat(s[0])+tmpx[1];
                tmpy[0] = Float.parseFloat(s[1])+tmpy[1];
              } else {
                tmpx[0] = Float.parseFloat(s[0]);
                tmpy[0] = Float.parseFloat(s[1]);
              }
              sType = "arrow";
              break;
            }
            String s[] = info[z+1].split(",");
            tmpx[1] = Float.parseFloat(s[0]);
            tmpy[1] = Float.parseFloat(s[1]);
            z++;                      
          }
          else if(info[z].contains("V")||info[z].contains("v")){
            tmpx[0] = tmpx[1];
            tmpy[0] = Float.parseFloat(info[z+1]);
            sType = "triangle";             
            z++;
          }
          else if(info[z].contains("C")||info[z].contains("c"))
            z+=2;
          else if(info[z].contains("H")){
            String s[] = info[z+1].split(",");
            tmpx[2] = Float.parseFloat(s[0]);
            tmpy[2] = tmpy[0];
            break;
          }
          else if(info[z].contains("h"))
            System.out.println("\tX-Destination point : "+info[z+1]);
          else if(info[z].contains("l")||info[z].contains("L")){
            System.out.println("\tCondition point : "+info[z+1]);
            sType = "diamond";
          } 
          else {
            String s[] = info[z].split(",");
            tmpx[0] = Float.parseFloat(s[0]);
            tmpy[0] = Float.parseFloat(s[1]);
            sType = "triangle";
          }
        }
      }
    }
    catch(SVGException e)
      System.out.println(e); 
    if(sType == "rectangle"){
      SVGNode temp = new Rectangle(element.getId(),tmpx[0],tmpy[0],tmpx[1],tmpy[1]);
      nodes.add(temp);
    } 
    else if (sType == "triangle"){
      SVGNode temp = new Triangle(element.getId(),tmpx,tmpy);
      nodes.add(temp);
    } 
    else if (sType == "text"){
      SVGNode temp = new Text(element.getId(),tText,tmpx[0],tmpy[0]);
      nodes.add(temp);
    }
  }
  public static class SVGNode
  {
    String iId;
    public SVGNode(String iId)
    {
      this.iId = iId;
    }
  }
  public static class Rectangle extends SVGNode
  {
    float iX;
    float iY;
    float iWidth;
    float iHeight;
    public Rectangle(String iId,float iX,float iY,float iWidth,float iHeight)
    {
      super(iId);
      this.iY = iY;
      this.iX = iX;
      this.iWidth = iWidth;
      this.iHeight = iHeight;
    }
    public void printAll()
    {
      System.out.println("\nID :"+iId);
      System.out.println("\ttype :"+this.getClass().getSimpleName());
      System.out.println("\tX : "+iX+" Y : "+iY);
      System.out.println("\tWidth : "+iWidth+" Height : "+iHeight);
    }
  }
  public static class Triangle extends SVGNode
  {
    float[] iAngleX = new float[4];
    float[] iAngleY = new float[4];;
    public Triangle(String iId,float iAngleX[],float iAngleY[])
    {
      super(iId);
      this.iAngleX = iAngleX.clone();
      this.iAngleY = iAngleY.clone();
    }
    public void printAll()
    {
      System.out.println("\nID :"+iId);
      System.out.println("\ttype :"+this.getClass().getSimpleName());
      for(int i=0;i<3;i++){
        System.out.println("\tNode#"+i+" X : "+iAngleX[i]+" Y : "+iAngleY[i]);              
      }
    }
  }
  public static class Text extends SVGNode
  {
    String sText;
    float iX;
    float iY;
    public Text(String iId,String sText,float iX,float iY)
    {
      super(iId);
      this.sText = sText;
      this.iX = iX;
      this.iY = iY;
    }
    public void printAll()
    {
      System.out.println("\nID :"+iId);
      System.out.println("\ttype :"+this.getClass().getSimpleName());
      System.out.println("\tX : "+iX+" Y : "+iY);
      System.out.println("\tText : "+sText);  
    }
  }
}
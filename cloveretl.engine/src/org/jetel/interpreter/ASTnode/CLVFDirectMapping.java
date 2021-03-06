/*
 * jETeL/CloverETL - Java based ETL application framework.
 * Copyright (c) Javlin, a.s. (info@cloveretl.com)
 *  
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jetel.interpreter.ASTnode;

import org.jetel.component.CustomizedRecordTransform;
import org.jetel.data.DataField;
import org.jetel.data.DataRecord;
import org.jetel.interpreter.ExpParser;
import org.jetel.interpreter.ParseException;
import org.jetel.interpreter.TransformLangExecutorRuntimeException;
import org.jetel.interpreter.TransformLangParserVisitor;
import org.jetel.metadata.DataRecordMetadata;

public class CLVFDirectMapping extends SimpleNode {
 
	public enum MappingType {
		Field2Field,
		Literal2Field,
		MultipleLiteral2Field
	}
	
    public DataField field;
    public DataField srcField;
    public int recordNo=-1;
    public int fieldNo=-1;
    public String fieldName;
    public int arity;
    public MappingType mappingType;
    
  public CLVFDirectMapping(int id) {
    super(id);
  }

  public CLVFDirectMapping(ExpParser p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  @Override
public Object jjtAccept(TransformLangParserVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }
  
  
  @Override 
  public void jjtClose(){
	  //optimize type of mapping
	  arity=jjtGetNumChildren();
	  if (arity==1){
		  Node aNode=jjtGetChild(0);
		  CLVFInputFieldLiteral childNode = aNode instanceof CLVFInputFieldLiteral ? (CLVFInputFieldLiteral)aNode : null;
		  if (childNode !=null &&  childNode.fieldNo>=0 && 
				  parser.getOutRecordMeta(recordNo).getField(fieldNo).getType() == parser.getInRecordMeta(childNode.recordNo).getField(childNode.fieldNo).getType()){
		  		  mappingType=MappingType.Field2Field;
		  		  srcField=childNode.field;
		  }else{
			  mappingType=MappingType.Literal2Field;
		  }
	  }else{
		  mappingType=MappingType.MultipleLiteral2Field;
	  }
  }
  
  
  /**
   * Get field of input record (1st record)
   * 
     * @param fName
     * @throws ParseException
     */
    public void setFieldName(String fName) throws ParseException{
        // get rid of leading '$' character (the 1st character)
      fieldName=fName;
      recordNo=0;
      DataRecordMetadata record= parser.getOutRecordMeta(0);
      if (record==null){
          throw new ParseException("Unknown output data field \""+fName+"\"");
      }
        fieldNo=record.getFieldPosition(fName.substring(1));
        if (fieldNo==-1){
            throw new ParseException("Unknown output data field \""+fName+"\"");
        }
    }
   public void setRecordFieldName(String fRecName) throws ParseException{
          // get rid of leading '$' character (the 1st character)
          fieldName=fRecName;   
          String recFieldName[]=fRecName.substring(1).split("\\.");
          DataRecordMetadata record;
          try{
        	  recordNo=parser.getOutRecordNum(recFieldName[0]);
              record=parser.getOutRecordMeta(recordNo);
          }catch(Exception ex){
              throw new ParseException("Error accessing record \""+recFieldName[0]+"\" "+ex.getMessage());
          }
          if (record==null){
              throw new ParseException("Unknown output record \""+recFieldName[0]+"\""); 
          }
          fieldNo=record.getFieldPosition(recFieldName[1]);
          if (fieldNo==-1){
              throw new ParseException("Unknown output data field ["+fRecName+"]");
          }
          
      }
   
   public void setRecordNumFieldName(String fRecName) throws ParseException{
       // get rid of leading '$' character (the 1st character)
       fieldName=fRecName;
       String recFieldName[]=fRecName.substring(1).split("\\.");
       DataRecordMetadata record=null;
       try{
    	   recordNo=Integer.parseInt(recFieldName[0]);
           record=parser.getOutRecordMeta(recordNo);
       }catch(NumberFormatException ignore){
       }
       if (record==null){
           throw new ParseException("Unknown output record \""+recFieldName[0]+"\""); 
       }
       fieldNo=record.getFieldPosition(recFieldName[1]);
       if (fieldNo==-1){
           throw new ParseException("Unknown output data field \""+fRecName+"\"");
       }
   }   
   
   
   public void setRecordNumFieldNum(String fRecFieldNum) throws ParseException { 
	   fieldName=fRecFieldNum;  
	   String items[]=fRecFieldNum.substring(1).split("\\.");
  	   recordNo=Integer.parseInt(items[0]);
       DataRecordMetadata record=parser.getInRecordMeta(recordNo);
       if (record==null){
           throw new ParseException("Unknown output data field ["+fRecFieldNum+"]"); 
       }
       try{
      	 fieldNo=Integer.parseInt(items[1]);
       }catch(Exception ex){
           throw new ParseException("Unknown output data field ["+fRecFieldNum+"]");
       }
   }
   
   public void setRecordNameFieldNum(String fRecName) throws ParseException {
	    // get rid of leading '$' character (the 1st character)
       fieldName=fRecName;   
       String recFieldName[]=fRecName.substring(1).split("\\.");
       DataRecordMetadata record;
       try{
     	  recordNo=parser.getOutRecordNum(recFieldName[0]);
           record=parser.getOutRecordMeta(recordNo);
       }catch(Exception ex){
           throw new ParseException("Error accessing record \""+recFieldName[0]+"\" "+ex.getMessage());
       }
       if (record==null){
           throw new ParseException("Unknown output record \""+recFieldName[0]+"\""); 
       }
       try{
        	 fieldNo=Integer.parseInt(recFieldName[1]);
       }catch(Exception ex){
             throw new ParseException("Unknown output data field ["+recFieldName[1]+"]");
        }
       if (record.getField(fieldNo)==null){
           throw new ParseException("Unknown output data field ["+recFieldName[1]+"]");
       }
   }
   
   
   public void bindToField(DataRecord[] records){
       try{
           field=records[recordNo].getField(fieldNo);
       }catch(NullPointerException ex){
           throw new TransformLangExecutorRuntimeException("can't determine "+fieldName);
       }
   }
   
   
   public void updateMappingMatrix(CustomizedRecordTransform custTrans){
	   custTrans.deleteRule(recordNo, fieldNo);
   }
   
   public void setArity(int arity){
       this.arity=arity;
   }
   
   @Override
public void dump(String prefix) {
	    System.out.println(toString(prefix));
	    if (children != null) {
	      for (int i = 0; i < children.length; ++i) {
		SimpleNode n = (SimpleNode)children[i];
		if (n != null) {
		  n.dump(prefix + " ");
		}
	      }
	    }
	    System.out.println("Rec:"+recordNo+" field: "+fieldNo+" fieldName:"+fieldName);
	  }
   
}

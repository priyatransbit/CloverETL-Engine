/* Generated By:JJTree: Do not edit this line. C:\projects\eclipse\workspace\svn_clover_engine\src\org\jetel\interpreter\FilterExpParserVisitor.java */

package org.jetel.interpreter;

public interface FilterExpParserVisitor
{
  public Object visit(SimpleNode node, Object data);
  public Object visit(CLVFStart node, Object data);
  public Object visit(CLVFOr node, Object data);
  public Object visit(CLVFAnd node, Object data);
  public Object visit(CLVFComparison node, Object data);
  public Object visit(CLVFAddNode node, Object data);
  public Object visit(CLVFSubNode node, Object data);
  public Object visit(CLVFMulNode node, Object data);
  public Object visit(CLVFDivNode node, Object data);
  public Object visit(CLVFModNode node, Object data);
  public Object visit(CLVFMinusNode node, Object data);
  public Object visit(CLVFNegation node, Object data);
  public Object visit(CLVFSubStrNode node, Object data);
  public Object visit(CLVFUppercaseNode node, Object data);
  public Object visit(CLVFLowercaseNode node, Object data);
  public Object visit(CLVFTrimNode node, Object data);
  public Object visit(CLVFLengthNode node, Object data);
  public Object visit(CLVFTodayNode node, Object data);
  public Object visit(CLVFIsNullNode node, Object data);
  public Object visit(CLVFNVLNode node, Object data);
  public Object visit(CLVFReplaceNode node, Object data);
  public Object visit(CLVFStr2NumNode node, Object data);
  public Object visit(CLVFNum2StrNode node, Object data);
  public Object visit(CLVFIffNode node, Object data);
  public Object visit(CLVFPrintErrNode node, Object data);
  public Object visit(CLVFLiteral node, Object data);
  public Object visit(CLVFJetelFieldLiteral node, Object data);
  public Object visit(CLVFRegexLiteral node, Object data);
  public Object visit(CLVFConcatNode node, Object data);
  public Object visit(CLVFDateAddNode node, Object data);
  public Object visit(CLVFDateDiffNode node, Object data);
}

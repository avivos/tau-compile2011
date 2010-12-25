package IC.SymbolTables;

import java.util.ArrayList;
import java.util.List;

import IC.AST.*;
import IC.TYPE.Kind;

public class SymbolTableConstructor implements Visitor {

   private String ICFilePath;
   private SymbolTable st;
   private SymbolTable currentScope;
   
 
   public SymbolTableConstructor(String ICFilePath) {
       this.ICFilePath = ICFilePath;
       this.st = new GlobalSymbolTable(ICFilePath);
       
   }
   
   public Object visit(Program program) {
       SemanticSymbol temp; 
       for (ICClass icClass : program.getClasses()) {
           temp = new SemanticSymbol(program.getSemanticType(), new Kind(Kind.CLASS), icClass.getName(), false);
           if (!st.insert(icClass.getName(),temp)) {  //Symbol already in symbol table
               System.out.println("Error: Illegal redefinition; element " + icClass.getName() + " in line #" + icClass.getLine());
               System.exit(-1);
           }
       }
       
       for (ICClass icClass : program.getClasses()) {
          currentScope = st;
          st.addChild((ClassSymbolTable)icClass.accept(this));
       }
       
       for (ICClass icClass : program.getClasses()) {
           if (icClass.hasSuperClass()) {
               SymbolTable son = st.removeChild(icClass.getName());
               String dad = icClass.getSuperClassName();
               SymbolTable dadTable = st.symbolTableLookup(dad);
               dadTable.addChild(son); 
           }
        }
       program.setEnclosingScope(st);
       return st;  
   }

   public Object visit(ICClass icClass) {
	   SymbolTable classTable = new ClassSymbolTable(icClass.getName());
	   currentScope = classTable;
       for (Field field : icClass.getFields())
    	   if (!classTable.insert(field.getName(), new SemanticSymbol(field.getSemanticType(), new Kind(Kind.FIELD), field.getName(), false))) { 
    	       System.out.println("Error: Illegal redefinition; element " + field.getName() + " in line #" + field.getLine());
               System.exit(-1);
    	   }

       for (Method method : icClass.getMethods()) {
           if (method instanceof VirtualMethod) { 
               boolean a = classTable.insert(method.getName(),new SemanticSymbol(method.getSemanticType(), new Kind(Kind.VIRTUALMETHOD),method.getName(),false));
               if (!a) { 
                   System.out.println("Error: Illegal redefinition; element " + method.getName() + " in line #" + method.getLine());
                   System.exit(-1);
               }
           }
           else {
               boolean a = classTable.insert(method.getName(),new SemanticSymbol(method.getSemanticType(), new Kind(Kind.STATICMETHOD),method.getName(),false));
               if (!a) { 
                   System.out.println("Error: Illegal redefinition; element " + method.getName() + " in line #" + method.getLine());
                   System.exit(-1);
               }
           }
       }
       
     
       for (Method method : icClass.getMethods()) {
           currentScope = classTable;
    	   classTable.addChild((MethodSymbolTable)method.accept(this));
       }
       
       icClass.setEnclosingScope(classTable);
       return classTable;
   }

   public Object visit(PrimitiveType type) {
       type.setEnclosingScope(currentScope);
       return null; 
   }

   public Object visit(UserType type) {
       type.setEnclosingScope(currentScope);
       return null;
   }

   public Object visit(Field field) {
       field.setEnclosingScope(currentScope);
       return null;
   }

   public Object visit(LibraryMethod method) {
       return handleMethod(method);
   }

   public Object visit(Formal formal) {
       formal.setEnclosingScope(currentScope);
       return null;   
   }

   public Object visit(VirtualMethod method) {
	   return handleMethod(method);
   }

   public Object visit(StaticMethod method) {
       return handleMethod(method);
   }

   public Object visit(Assignment assignment) {
       assignment.setEnclosingScope(currentScope);
       assignment.getAssignment().accept(this);
       assignment.getVariable().accept(this);
	   return null;
   }

   public Object visit(CallStatement callStatement) {
       callStatement.getCall().accept(this);
       callStatement.setEnclosingScope(currentScope);
       
	   return null;
   }

   public Object visit(Return returnStatement) {       //non-scoped
       if (returnStatement.hasValue())
           returnStatement.getValue().accept(this);
       returnStatement.setEnclosingScope(currentScope);
       return null;
   }

   public Object visit(If ifStatement) {        
      SymbolTable ifSymbolTable = new BlockSymbolTable("if");
      currentScope = ifSymbolTable;
      ifSymbolTable.addChild((SymbolTable)ifStatement.getOperation().accept(this));

       if (ifStatement.hasElse()) { 
    	   currentScope = ifSymbolTable;
           ifSymbolTable.addChild((SymbolTable)ifStatement.getElseOperation().accept(this));
       }
       ifStatement.setEnclosingScope(ifSymbolTable); //TODO: maybe currentScope
      return ifSymbolTable;
   }

   public Object visit(While whileStatement) {
      SymbolTable whileSymbolTable = new SymbolTable("while");
      whileSymbolTable.setLoop(true);
      currentScope = whileSymbolTable;
      whileSymbolTable.addChild((SymbolTable)whileStatement.getOperation().accept(this));
      whileStatement.setEnclosingScope(whileSymbolTable); //TODO: maybe currentScope
      return whileSymbolTable;
   }

   public Object visit(Break breakStatement) {       
       breakStatement.setEnclosingScope(currentScope);
       return null;
   }

   public Object visit(Continue continueStatement) {      
       continueStatement.setEnclosingScope(currentScope);
       return null;
   }

   public Object visit(StatementsBlock statementsBlock) {
       SymbolTable blockTable = new BlockSymbolTable("block"); // TODO: want this ID to be statement block in "sfunc"
       SymbolTable symbolTable;
       
       for (Statement statement : statementsBlock.getStatements()) { 
           currentScope = blockTable; // or symbolTable;
           if (statement instanceof LocalVariable) {
               LocalVariable lv = (LocalVariable)statement;
              
               boolean insertSuccessful = blockTable.insert(lv.getName(), new SemanticSymbol(statement.getSemanticType(), new Kind(Kind.VAR), lv.getName(), false));
               if (!insertSuccessful) { 
                   System.out.println("Error: Illegal redefinition; element " + lv.getName() + " in line #" + lv.getLine());
                   System.exit(-1);
               }
               statement.setEnclosingScope(blockTable);
           }
           else {
               symbolTable = (SymbolTable)statement.accept(this);
               if (symbolTable != null){
                   blockTable.addChild((SymbolTable)statement.accept(this));
               }
           }
       }
       
       statementsBlock.setEnclosingScope(blockTable);
       currentScope = blockTable;
       return blockTable;
       
   }

   public Object visit(LocalVariable localVariable) {
       localVariable.setEnclosingScope(currentScope);
       if (localVariable.hasInitValue()) { 
           //localVariable.getInitValue().setEnclosingScope(currentScope);
           localVariable.getInitValue().accept(this);
       }
       return null;
   }

   public Object visit(VariableLocation location) {
       location.setEnclosingScope(currentScope);
       if (location.getLocation() != null) 
           location.getLocation().accept(this);
       return null;
   }

   public Object visit(ArrayLocation location) {
       location.setEnclosingScope(currentScope);
       if (location.getArray() != null) 
           location.getArray().accept(this);
       return null;
   }

   public Object visit(StaticCall call) {
	   for (Expression e : call.getArguments()){
    	   e.accept(this);
       }
       call.setEnclosingScope(currentScope);
       return null;
   }

   public Object visit(VirtualCall call) {
	   for (Expression e : call.getArguments()){
    	   e.accept(this);
       }
       call.setEnclosingScope(currentScope);
       return null;
   }

   public Object visit(This thisExpression) {
       thisExpression.setEnclosingScope(currentScope);
       return null;
   }

   public Object visit(NewClass newClass) {
       newClass.setEnclosingScope(currentScope);
       return null; 
   }

   public Object visit(NewArray newArray) {
       newArray.setEnclosingScope(currentScope);
       return null;
   }

   public Object visit(Length length) {
       length.setEnclosingScope(currentScope);
       length.getArray().setEnclosingScope(currentScope);
       return null;
   }

   public Object visit(MathBinaryOp binaryOp) {
       binaryOp.setEnclosingScope(currentScope);
       return null;
   }

   public Object visit(LogicalBinaryOp binaryOp) {
       binaryOp.setEnclosingScope(currentScope);
       return null;
   }

   public Object visit(MathUnaryOp unaryOp) {
       unaryOp.setEnclosingScope(currentScope);
       return null;
   }

   public Object visit(LogicalUnaryOp unaryOp) {
       unaryOp.setEnclosingScope(currentScope);
       return null;
   }

   public Object visit(Literal literal) {
       literal.setEnclosingScope(currentScope);
       return null;
   }

   public Object visit(ExpressionBlock expressionBlock) {
       expressionBlock.getExpression().accept(this);
       expressionBlock.setEnclosingScope(currentScope);
       return null;
   }
   
   private Object handleMethod(Method method) {
	   SymbolTable methodTable = new MethodSymbolTable(method.getName());
	   SymbolTable symbolTable;//child to be
	   currentScope = methodTable;
       if (method.getFormals().size() > 0) {
           for (Formal formal : method.getFormals()) { 
               boolean insertSuccessfully = methodTable.insert(formal.getName(), new SemanticSymbol(formal.getSemanticType(), new Kind(Kind.FORMAL), formal.getName(), false));
               if (!insertSuccessfully) { 
                   System.out.println("Error: Illegal redefinition; element " + formal.getName() + " in line #" + formal.getLine());
                   System.exit(-1);
               }
           }
       }
	   for (Statement statement : method.getStatements()) { 
           if(statement instanceof LocalVariable){
        	   LocalVariable lv = (LocalVariable)statement;
        	   lv.accept(this);
        	   boolean insertSuccessfully = methodTable.insert(lv.getName(), new SemanticSymbol(statement.getSemanticType(), new Kind(Kind.VAR), lv.getName(), false));
        	   if (!insertSuccessfully) { 
                   System.out.println("Error: Illegal redefinition; element " + lv.getName() + " in line #" + lv.getLine());
                   System.exit(-1);
               }
           }
           else {
        	   symbolTable = (SymbolTable)statement.accept(this);
        	   if (symbolTable != null){
        		   methodTable.addChild((SymbolTable)statement.accept(this));
        	   }
        	   currentScope = methodTable;
           }
       }
       method.setEnclosingScope(methodTable);	
       currentScope = methodTable;
       return methodTable;
}
   
   
   
}

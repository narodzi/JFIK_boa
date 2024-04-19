import java.util.HashMap;
import java.util.LinkedList;
import java.util.Stack;
import java.util.HashSet;

import javax.sound.midi.SysexMessage;

import java.util.ArrayList;

enum VarType{ INT, REAL, BOOLEAN, STRING, STRUCTURE, FUNCTION, UNKNOWN }

class Value{ 
	public String name;
	public VarType type;
   public int length;
	public Value( String name, VarType type, int length ){
		this.name = name;
		this.type = type;
      this.length = length;
	}
}

class Structure {
   public String structureTypeName;
   public HashMap<String, Value> variablesNames = new HashMap<String, Value>();
   public ArrayList<String> definedStructures = new ArrayList<String>();

   public Structure(String name) {
      this.structureTypeName = name;
   }
}

public class LLVMActions extends boaBaseListener {
    HashSet<String> functions = new HashSet<String>();
    HashSet<String> localnames = new HashSet<String>();
    String value, function;
    Boolean global;

    HashMap<String, VarType> variables = new HashMap<String, VarType>();
    HashMap<String, VarType> local_variables = new HashMap<String, VarType>();
    Stack<Value> stack = new Stack<Value>();

    HashMap<String, Structure> structures = new HashMap<String, Structure>();

    static int BUFFER_SIZE = 16;


   // STRUCTURES ZONE
   @Override
   public void exitDefStruct(boaParser.DefStructContext ctx) {
      Structure newStructureDefinition = new Structure(ctx.getChild(1).getText());
      for(int i = 4; i < ctx.getChildCount()-2; i += 3) {
         String type = ctx.getChild(i).getText();
         if(!type.equals("int") && !type.equals("real") && !type.equals("bool")) {
            error(ctx.getStart().getLine(), "Declared variable has unknown type");
         }
         String varName = ctx.getChild(i+1).getText();

         switch(type) {
            case "int":
               newStructureDefinition.variablesNames.put(varName, new Value(varName, VarType.INT, 0));
            break;
            case "real":
               newStructureDefinition.variablesNames.put(varName, new Value(varName, VarType.REAL, 0));
            break;
            case "bool":
               newStructureDefinition.variablesNames.put(varName, new Value(varName, VarType.BOOLEAN, 0));
            break;
         }
      }
      structures.put(newStructureDefinition.structureTypeName, newStructureDefinition);

      LLVMGenerator.begin_struct_declaration(newStructureDefinition.structureTypeName);
      for(Value value: newStructureDefinition.variablesNames.values()) {
         if(value.type == VarType.INT) {
            LLVMGenerator.declare_i32_struct_variable();
         }
         if(value.type == VarType.REAL) {
            LLVMGenerator.declare_double_struct_variable();
         }
         if(value.type == VarType.BOOLEAN) {
            LLVMGenerator.declare_boolean_struct_variable();
         }
      }
      LLVMGenerator.end_struct_declaration();
   }      

   @Override
   public void exitNewStruct(boaParser.NewStructContext ctx) {
      String variableName = ctx.getChild(0).getText();
      String structureName = ctx.getChild(2).getText();
      if(structures.containsKey(structureName)) {
         LLVMGenerator.define_structure(variableName, structureName);
         String llvmStructureName = "%" + variableName;

         int index = 0;
         for(Value variable : structures.get(structureName).variablesNames.values()) {
            LinkedList<Value> list = new LinkedList<>(stack);
            Value v = list.removeLast();
            // Value v = stack.reversed().removeLast();
            String llvmVariableName = llvmStructureName + "_" + variable.name;

            if(variable.name.contains("%")) {
               llvmVariableName = llvmStructureName + "_" + variable.name.replace("%", "pr");
            }

            LLVMGenerator.assign_value_to_structure_variable(structureName, llvmStructureName, llvmVariableName, index);
            switch(v.type) {
               case INT:
                  LLVMGenerator.assign_i32('%' + llvmVariableName.substring(1), v.name);
               break;
               case REAL:
                  LLVMGenerator.assign_double('%' + llvmVariableName.substring(1), v.name);
               break;
               case BOOLEAN:
                  LLVMGenerator.assign_boolean('%' + llvmVariableName.substring(1), v.name);
               break;
               default:
                  error(ctx.getStart().getLine(), "invalid type");
               break;
            }
            index++;
         }

         if(!structures.get(structureName).definedStructures.contains(variableName)) {
            structures.get(structureName).definedStructures.add(variableName);
            variables.put(variableName, VarType.STRUCTURE);
         } else {
            error(ctx.getStart().getLine(), "Duplicated variable name");
         }

      } else { // nie znaleziono struktury o takiej nazwie
         error(ctx.getStart().getLine(), "Mismatched structure type");
      }
   }

   @Override
   public void exitIdVal(boaParser.IdValContext ctx) {
      String ID = ctx.ID().getText();
      if(ID.contains(".")) {
         String structureVarName = ID.substring(0, ID.indexOf("."));
         String nestedVariableName = ID.substring(ID.indexOf(".")+1);
         boolean definitionNotFound = false;

         for(Structure s : structures.values()) {
            if(s.definedStructures.contains(structureVarName)) {
               Value nestedVar = s.variablesNames.get(nestedVariableName);
               if(nestedVar != null) {
                  switch(nestedVar.type) {
                     case INT:
                        LLVMGenerator.load_i32("%" + structureVarName + '_' + nestedVariableName);
                        stack.push( new Value("%" + (LLVMGenerator.reg-1) , VarType.INT, 0) );    
                     break;
                     case REAL:
                        LLVMGenerator.load_double('%' + structureVarName + '_' + nestedVariableName);
                        stack.push( new Value("%" + (LLVMGenerator.reg-1) , VarType.REAL, 0) );       
                     break;
                     case BOOLEAN:
                        LLVMGenerator.load_boolean('%' + structureVarName + '_' + nestedVariableName);
                        stack.push( new Value("%" + (LLVMGenerator.reg-1) , VarType.BOOLEAN, 0) );       
                     break;
                     default: 
                  }
               } else {
                  error(ctx.getStart().getLine(), "Invalid " + nestedVariableName + " nested variable name of structure "+ s.structureTypeName);
               }
   
            } else {
               definitionNotFound = true;
            }

         }

         if(definitionNotFound) {
            error(ctx.getStart().getLine(), structureVarName + "  has not been defined");
         }
      } else {
        String newID = "@"+ID;
        VarType type = variables.get(newID);
        if(type != null && !global) { 
            newID = "%"+ID;
            type = local_variables.get(newID); 
        }
        if(type != null && !global) { 
         newID = "%"+ID;
         type = local_variables.get(newID); 
        }   
        ID = newID;
         if(variables.containsKey(ID) || local_variables.containsKey(ID)) {
            switch(variables.get(ID) != null ? variables.get(ID) : local_variables.get(ID)) {
               case INT:
                  LLVMGenerator.load_i32(ID);
                  stack.push( new Value("%" + (LLVMGenerator.reg-1) , VarType.INT, 0) );     
               break;
               case REAL:
                  LLVMGenerator.load_double(ID);
                  stack.push( new Value("%" + (LLVMGenerator.reg-1) , VarType.REAL, 0) );        
               break;
               case BOOLEAN:
                  LLVMGenerator.load_boolean(ID);
                  stack.push( new Value("%" + (LLVMGenerator.reg-1) , VarType.BOOLEAN, 0) );          
               break; 
               default: 
            }
         } else {
            error(ctx.getStart().getLine(), ID + " not exist");
         }

      }
   }

   // STRUCTURES ZONE END

   @Override
   public void enterProg(boaParser.ProgContext ctx) {
      global = true;
   }

    @Override 
    public void exitProg(boaParser.ProgContext ctx) { 
       LLVMGenerator.close_main();
       System.out.println( LLVMGenerator.generate() );
    }

    @Override
    public void exitAssign(boaParser.AssignContext ctx) { 
       String ID = ctx.ID().getText();
       Value v = stack.pop();

      if(ID.contains(".")) {
         String structureVarName = ID.substring(0, ID.indexOf("."));
         String nestedVariableName = ID.substring(ID.indexOf(".")+1);
         boolean definitionNotFound = false;

         for(Structure s : structures.values()) {
            if(s.definedStructures.contains(structureVarName)) {
               Value nestedVar = s.variablesNames.get(nestedVariableName);
               if(nestedVar != null) {
                  switch(nestedVar.type) {
                     case INT:
                        LLVMGenerator.assign_i32('%' + structureVarName + '_' + nestedVariableName, v.name);
                     break;
                     case REAL:
                        LLVMGenerator.assign_double('%' + structureVarName + '_' + nestedVariableName, v.name);
                     break;
                     case BOOLEAN:
                        LLVMGenerator.assign_boolean('%' + structureVarName + '_' + nestedVariableName, v.name);
                     break;
                     default: 
                  }
               } else {
                  error(ctx.getStart().getLine(), "Invalid " + nestedVariableName + " nested variable name of structure "+ s.structureTypeName);
               }
   
            } else {
               definitionNotFound = true;
            }

         }

         if(definitionNotFound) {
            error(ctx.getStart().getLine(), structureVarName + "  has not been defined");
         }

      } else {
         ID = global ? "@" + ID : "%" + ID;
         if(!variables.containsKey(ID)) {  
            if(global){        
            variables.put(ID, v.type);
            if( v.type == VarType.INT ){
            LLVMGenerator.declare_global_i32(ID);
            LLVMGenerator.assign_i32(ID, v.name);
            } 
            if( v.type == VarType.REAL ){
            LLVMGenerator.declare_global_double(ID);
            LLVMGenerator.assign_double(ID, v.name);
            } 
            if( v.type == VarType.STRING ){
            LLVMGenerator.declare_global_string(ID);
            LLVMGenerator.assign_string(ID);
            }
            if (v.type == VarType.BOOLEAN) {
            LLVMGenerator.declare_global_boolean(ID);
            LLVMGenerator.assign_boolean(ID, v.name);
            }
         }
         else{
            local_variables.put(ID, v.type);
               if( v.type == VarType.INT ){
               LLVMGenerator.declare_i32(ID);
               LLVMGenerator.assign_i32(ID, v.name);
               } 
               if( v.type == VarType.REAL ){
               LLVMGenerator.declare_double(ID);
               LLVMGenerator.assign_double(ID, v.name);
               } 
               if( v.type == VarType.STRING ){
               LLVMGenerator.declare_string(ID);
               LLVMGenerator.assign_string(ID);
               }
               if (v.type == VarType.BOOLEAN) {
               LLVMGenerator.declare_boolean(ID);
               LLVMGenerator.assign_boolean(ID, v.name);
            }
         }
      } 
         else {
            if( v.type == VarType.INT ){
               if(variables.get(ID) == VarType.INT) {
                  LLVMGenerator.assign_i32(ID, v.name);
               } else {
                  error(ctx.getStart().getLine(), ID + " is type int, invalid assignment operation");
               }
             } 
             if( v.type == VarType.REAL ){
               if(variables.get(ID) == VarType.REAL) {
                  LLVMGenerator.assign_double(ID, v.name);
               } else {
                  error(ctx.getStart().getLine(), ID + " is type real, invalid assignment operation");
               }
             } 
             if( v.type == VarType.STRING ){
               if(variables.get(ID) == VarType.STRING) {
                  LLVMGenerator.assign_string(ID);
               } else {
                  error(ctx.getStart().getLine(), ID + " is type real, invalid assignment operation");
               }
             }
             if (v.type == VarType.BOOLEAN) {
               if(variables.get(ID) == VarType.BOOLEAN) {
                  LLVMGenerator.assign_string(ID);
               } else {
                  error(ctx.getStart().getLine(), ID + " is type boolean, invalid assignment operation");
               }
            }
         }
         }
      }
    

    @Override 
    public void exitInt(boaParser.IntContext ctx) { 
         stack.push( new Value(ctx.INT().getText(), VarType.INT, 0) );       
    } 

    @Override 
    public void exitReal(boaParser.RealContext ctx) { 
         stack.push( new Value(ctx.REAL().getText(), VarType.REAL, 0) );       
    } 

    @Override
    public void exitString(boaParser.StringContext ctx) {
       String tmp = ctx.STRING().getText(); 
       String content = tmp.substring(1, tmp.length()-1);
       LLVMGenerator.constant_string(content);
       String n = "ptrstr" + (LLVMGenerator.str-1);
       stack.push( new Value(n, VarType.STRING, content.length()) );
    }

    @Override
    public void exitBoolean(boaParser.BooleanContext ctx) {
       stack.push(new Value(ctx.BOOLEAN().getText(), VarType.BOOLEAN, 0));
    }

    @Override 
    public void exitAdd(boaParser.AddContext ctx) { 
       Value v1 = stack.pop();
       Value v2 = stack.pop();
       if( v1.type == v2.type ) {
	  if( v1.type == VarType.INT ){
             LLVMGenerator.add_i32(v1.name, v2.name); 
             stack.push( new Value("%"+(LLVMGenerator.reg-1), VarType.INT, 0) ); 
          }
	  if( v1.type == VarType.REAL ){
             LLVMGenerator.add_double(v1.name, v2.name); 
             stack.push( new Value("%"+(LLVMGenerator.reg-1), VarType.REAL, 0) ); 
         }
       } else {
          error(ctx.getStart().getLine(), "add type mismatch");
       }
    }

    @Override 
    public void exitSub(boaParser.SubContext ctx) { 
       Value v2 = stack.pop();
       Value v1 = stack.pop();
       if( v1.type == v2.type ) {
	  if( v1.type == VarType.INT ){
             LLVMGenerator.sub_i32(v1.name, v2.name); 
             stack.push( new Value("%"+(LLVMGenerator.reg-1), VarType.INT, 0) ); 
          }
	  if( v1.type == VarType.REAL ){
             LLVMGenerator.sub_double(v1.name, v2.name); 
             stack.push( new Value("%"+(LLVMGenerator.reg-1), VarType.REAL, 0) ); 
         }
       } else {
          error(ctx.getStart().getLine(), "sub type mismatch");
       }
    }

    @Override 
    public void exitMult(boaParser.MultContext ctx) { 
       Value v1 = stack.pop();
       Value v2 = stack.pop();
       if( v1.type == v2.type ) {
	  if( v1.type == VarType.INT ){
             LLVMGenerator.mult_i32(v1.name, v2.name); 
             stack.push( new Value("%"+(LLVMGenerator.reg-1), VarType.INT, 0) ); 
          }
	  if( v1.type == VarType.REAL ){
             LLVMGenerator.mult_double(v1.name, v2.name); 
             stack.push( new Value("%"+(LLVMGenerator.reg-1), VarType.REAL, 0) ); 
         }
       } else {
          error(ctx.getStart().getLine(), "mult type mismatch");
       }
    }

    @Override 
    public void exitDiv(boaParser.DivContext ctx) { 
       Value v2 = stack.pop();
       Value v1 = stack.pop();
       if( v1.type == v2.type ) {
	  if( v1.type == VarType.INT ){
             LLVMGenerator.div_i32(v1.name, v2.name); 
             stack.push( new Value("%"+(LLVMGenerator.reg-1), VarType.INT, 0) ); 
          }
	  if( v1.type == VarType.REAL ){
             LLVMGenerator.div_double(v1.name, v2.name); 
             stack.push( new Value("%"+(LLVMGenerator.reg-1), VarType.REAL, 0) ); 
         }
       } else {
          error(ctx.getStart().getLine(), "div type mismatch");
       }
    }
    
    @Override
   public void exitAnd(boaParser.AndContext ctx) {
      Value v1 = stack.pop();
      Value v2 = stack.pop();
      if (v1.type == v2.type) {
         if (v1.type == VarType.BOOLEAN) {
            LLVMGenerator.and_boolean(v1.name, v2.name);
            stack.push(new Value("%" + (LLVMGenerator.reg - 1), VarType.BOOLEAN, 0));
         }
      } else {
         error(ctx.getStart().getLine(), "and boolean type mismatch");
      }
   }

   @Override
   public void exitOr(boaParser.OrContext ctx) {
      Value v1 = stack.pop();
      Value v2 = stack.pop();
      if (v1.type == v2.type) {
         if (v1.type == VarType.BOOLEAN) {
            LLVMGenerator.or_boolean(v1.name, v2.name);
            stack.push(new Value("%" + (LLVMGenerator.reg - 1), VarType.BOOLEAN, 0));
         }
      } else {
         error(ctx.getStart().getLine(), "or boolean type mismatch");
      }
   }

   @Override
   public void exitXor(boaParser.XorContext ctx) {
      Value v1 = stack.pop();
      Value v2 = stack.pop();
      if (v1.type == v2.type) {
         if (v1.type == VarType.BOOLEAN) {
            LLVMGenerator.xor_boolean(v1.name, v2.name);
            stack.push(new Value("%" + (LLVMGenerator.reg - 1), VarType.BOOLEAN, 0));
         }
      } else {
         error(ctx.getStart().getLine(), "xor boolean type mismatch");
      }
   }

   @Override
   public void exitNeg(boaParser.NegContext ctx) {
      Value val = stack.pop();
      if (val.type == VarType.BOOLEAN) {
         LLVMGenerator.neg_boolean(val.name);
         stack.push(new Value("%" + (LLVMGenerator.reg - 1), VarType.BOOLEAN, 0));

      } else {
         error(ctx.getStart().getLine(), "neg boolean type mismatch");
      }
   }

   @Override
   public void exitSceand(boaParser.SceandContext ctx) {
      Value v1 = stack.pop();
      Value v2 = stack.pop();
      if (v1.type == v2.type) {
         if (v1.type == VarType.BOOLEAN) {
            if(v2.name.equals("false")) {
               LLVMGenerator.assign_false();
               stack.push(new Value("%" + (LLVMGenerator.reg - 1), VarType.BOOLEAN, 0));
            } else {               
               LLVMGenerator.and_boolean(v1.name, v2.name);
               stack.push(new Value("%" + (LLVMGenerator.reg - 1), VarType.BOOLEAN, 0));
            }
         }
      } else {
         error(ctx.getStart().getLine(), "short circuit evaluation and boolean type mismatch");
      }
   }

   @Override
   public void exitSceor(boaParser.SceorContext ctx) {
      Value v1 = stack.pop();
      Value v2 = stack.pop();
      if (v1.type == v2.type) {
         if (v1.type == VarType.BOOLEAN) {
            if(v2.name.equals("true")) {
               LLVMGenerator.assign_true();
               stack.push(new Value("%" + (LLVMGenerator.reg - 1), VarType.BOOLEAN, 0));
            } else {               
               LLVMGenerator.or_boolean(v1.name, v2.name);
               stack.push(new Value("%" + (LLVMGenerator.reg - 1), VarType.BOOLEAN, 0));
            }
         }
      } else {
         error(ctx.getStart().getLine(), "short circuit evaluation or boolean type mismatch");
      }
   }

   @Override
   public void exitBlock(boaParser.BlockContext ctx) {
      if( ctx.getParent() instanceof boaParser.RepeatContext ){
         LLVMGenerator.repeatEnd();
     }
   }
   
   @Override
   public void exitRepetitions(boaParser.RepetitionsContext ctx) {
       String value = "";
       if(ctx.ID() != null) {
           String ID = ctx.ID().getText();
           if(variables.containsKey(ID)) {
               if(variables.get(ID).equals("int")) {
                   LLVMGenerator.load_i32(ID);
                   value = "%" + (LLVMGenerator.reg - 1);
               } else {
                   error(ctx.getStart().getLine(), "Mismatch type in loop");
               }
           } else {
               error(ctx.getStart().getLine(), "Unknown variable "+ID);
           }
       } else if (ctx.INT() != null) {
           value = ctx.INT().getText();
       }
       LLVMGenerator.repeatStart(value);
   }

   @Override
    public void exitIf(boaParser.IfContext ctx) {
    }

    @Override
    public void enterBlockif(boaParser.BlockifContext ctx) {
        LLVMGenerator.ifstart();
    }

    @Override
    public void exitBlockif(boaParser.BlockifContext ctx) {
        LLVMGenerator.ifend();
    }

    @Override
    public void exitEqual(boaParser.EqualContext ctx) {
        String ID = ctx.ID().getText();
        ID = global ? "@" + ID : "%" + ID;
        String INT = ctx.INT().getText();
        if( variables.containsKey(ID) ) {
            LLVMGenerator.icmp( ID, INT );
        } else {
            ctx.getStart().getLine();
            System.err.println("Line "+ ctx.getStart().getLine()+", unknown variable: "+ID);
        }
    }

    @Override
    public void exitFparam(boaParser.FparamContext ctx) {
        String ID = ctx.ID().getText(); 
        if (!variables.containsKey(ID)) { 
            variables.put(ID, VarType.FUNCTION); 
            functions.add(ID); 
            function = ID; 
            LLVMGenerator.functionstart(ID);
        } else { 
            error(ctx.getStart().getLine(), "Name " + ID + " already declared");
        }
    }

    @Override
    public void enterFblock(boaParser.FblockContext ctx) {
        global = false; 
    }

    @Override
    public void exitFblock(boaParser.FblockContext ctx) {
        String ID = function;
        if(!local_variables.containsKey(function) ){ 
         local_variables.put(function, VarType.FUNCTION);
            LLVMGenerator.declare_i32("%"+ID);
            LLVMGenerator.assign_i32("%"+ID, "0"); 
        }
        LLVMGenerator.load_i32( "%"+ID ); 
        LLVMGenerator.functionend(); 
        local_variables = new HashMap<String, VarType>();
        global = true; 
    }

    @Override
    public void exitCall(boaParser.CallContext ctx) {
        String ID = ctx.ID().getText();
        if(functions.contains(ID)) {
            LLVMGenerator.call(ID);
        } else {
            error(ctx.getStart().getLine(), ID + " is not a fuction");
        }
    }

    @Override 
    public void exitToint(boaParser.TointContext ctx) { 
       Value v = stack.pop();
       LLVMGenerator.fptosi( v.name );
       stack.push( new Value("%"+(LLVMGenerator.reg-1), VarType.INT, 0) ); 
    }

    @Override 
    public void exitToreal(boaParser.TorealContext ctx) { 
       Value v = stack.pop();
       LLVMGenerator.sitofp( v.name );
       stack.push( new Value("%"+(LLVMGenerator.reg-1), VarType.REAL, 0) ); 
    }

    @Override
    public void exitReadint(boaParser.ReadintContext ctx){
      String ID = ctx.ID().getText();
      ID = global ? "@" + ID : "%" + ID;
      if( ! variables.containsKey(ID) && global) {
         variables.put(ID, VarType.INT);
         LLVMGenerator.declare_global_i32(ID);      
      }
      else if( ! local_variables.containsKey(ID) && !global) {
         local_variables.put(ID, VarType.INT);
         LLVMGenerator.declare_i32(ID);      
      }
      LLVMGenerator.scanf_i32(ID);
    }

    @Override 
    public void exitReadreal(boaParser.ReadrealContext ctx){
      String ID = ctx.ID().getText();
      ID = global ? "@" + ID : "%" + ID;
      if( ! variables.containsKey(ID) && global) {
         variables.put(ID, VarType.REAL);
         LLVMGenerator.declare_global_double(ID);     
      }
      else if( ! local_variables.containsKey(ID) && !global) {
         local_variables.put(ID, VarType.REAL);
         LLVMGenerator.declare_double(ID);     
      } 
      LLVMGenerator.scanf_double(ID);
    }

    @Override 
    public void exitReadbool(boaParser.ReadboolContext ctx){
      String ID = ctx.ID().getText();
      if( ! variables.containsKey(ID) ) {
         variables.put(ID, VarType.BOOLEAN);
         LLVMGenerator.declare_boolean(ID);     
      }
      LLVMGenerator.scanf_boolean(ID);
    }

    @Override
    public void exitReadstr(boaParser.ReadstrContext ctx){
      String ID = ctx.ID().getText();
      ID = global ? "@" + ID : "%" + ID;
      if( ! variables.containsKey(ID) && global) {
         variables.put(ID, VarType.STRING);
      }
      else if( ! local_variables.containsKey(ID) && !global) {
         local_variables.put(ID, VarType.STRING);
      }
       LLVMGenerator.scanf_string(ID, BUFFER_SIZE);
    }

    @Override
    public void exitWrite(boaParser.WriteContext ctx) {
      String ID = ctx.ID().getText(); 
      String newID = "@"+ID;
      VarType type = variables.get(newID);
      if(type == null && !global) { 
          newID = "%"+ID;
          type = local_variables.get(newID); 
      }
      if(type != null && !global) { 
         newID = "%"+ID;
         type = local_variables.get(newID); 
     }
      ID = newID;
      if (type == null) { 
          ID = ctx.ID().getText();
          type = variables.get(ID);
      }
       if( type != null ) {
          if( type == VarType.INT ){
            LLVMGenerator.printf_i32( ID );
          }
          if( type == VarType.REAL ){
            LLVMGenerator.printf_double( ID );
          }
          if( type == VarType.STRING ){
            LLVMGenerator.printf_string( ID );
          }
          if (type == VarType.BOOLEAN) {
            LLVMGenerator.printf_boolean(ID);
         }
       } else if(ID.contains(".")) {
         String structureVarName = ID.substring(0, ID.indexOf("."));
         String nestedVariableName = ID.substring(ID.indexOf(".")+1);
         boolean definitionNotFound = false;

         for(Structure s : structures.values()) {
            if(s.definedStructures.contains(structureVarName)) {
               Value nestedVar = s.variablesNames.get(nestedVariableName);
               if(nestedVar != null) {
                  switch(nestedVar.type) {
                     case INT:
                        LLVMGenerator.printf_i32('%' + structureVarName + '_' + nestedVariableName);
                     break;
                     case REAL:
                        LLVMGenerator.printf_double('%' + structureVarName + '_' + nestedVariableName);
                     break;
                     case BOOLEAN:
                        LLVMGenerator.printf_boolean('%' + structureVarName + '_' + nestedVariableName);
                     break;
                     default: 
                  }
               } else {
                  error(ctx.getStart().getLine(), "Invalid " + nestedVariableName + " nested variable name of structure "+ s.structureTypeName);
               }
   
            } else {
               definitionNotFound = true;
            }

         }

         if(definitionNotFound) {
            error(ctx.getStart().getLine(), structureVarName + "  has not been defined");
         }

      } else {
          error(ctx.getStart().getLine(), "unknown variable "+ID);
       }
    } 

   void error(int line, String msg){
       System.err.println("Error, line "+line+", "+msg);
       System.exit(1);
   } 
}
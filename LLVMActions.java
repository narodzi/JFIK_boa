import java.util.HashMap;
import java.util.Stack;

enum VarType{ INT, REAL, BOOLEAN, STRING, UNKNOWN }

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

public class LLVMActions extends boaBaseListener {

    HashMap<String, VarType> variables = new HashMap<String, VarType>();
    Stack<Value> stack = new Stack<Value>();

    static int BUFFER_SIZE = 16;

    @Override 
    public void exitProg(boaParser.ProgContext ctx) { 
       System.out.println(); // todo remove
       System.out.println( LLVMGenerator.generate() );
    }

    @Override
    public void exitAssign(boaParser.AssignContext ctx) { 
       String ID = ctx.ID().getText();
       Value v = stack.pop();
       variables.put(ID, v.type);
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
         LLVMGenerator.assing_boolean(ID, v.name);
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
     if( v1.type == VarType.STRING){
             LLVMGenerator.add_string(v1.name, v1.length, v2.name, v2.length);
             stack.push(new Value("%"+(LLVMGenerator.reg-3), VarType.STRING, v1.length) );   
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

      System.out.print("sceand");
      System.out.print(v1.name);
      System.out.print(v2.name);
      if (v1.type == v2.type) {
         if (v1.type == VarType.BOOLEAN) {
            System.out.print(v2.name);
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
      if( ! variables.containsKey(ID) ) {
         variables.put(ID, VarType.INT);
         LLVMGenerator.declare_i32(ID);      
      }
      LLVMGenerator.scanf_i32(ID);
    }

    @Override 
    public void exitReadreal(boaParser.ReadrealContext ctx){
      String ID = ctx.ID().getText();
      if( ! variables.containsKey(ID) ) {
         System.out.println(variables.containsKey(ID));
         variables.put(ID, VarType.REAL);
         LLVMGenerator.declare_double(ID);     
      }
      LLVMGenerator.scanf_double(ID);
    }

    @Override
    public void exitReadstr(boaParser.ReadstrContext ctx){
      String ID = ctx.ID().getText();
      if( ! variables.containsKey(ID) ) {
         System.out.println(variables.containsKey(ID));
         variables.put(ID, VarType.STRING);
      }
       LLVMGenerator.scanf_string(ID, BUFFER_SIZE);
    }

    @Override
    public void exitWrite(boaParser.WriteContext ctx) {
       String ID = ctx.ID().getText();
       VarType type = variables.get(ID);
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
       } else {
          error(ctx.getStart().getLine(), "unknown variable "+ID);
       }
    } 

   void error(int line, String msg){
       System.err.println("Error, line "+line+", "+msg);
       System.exit(1);
   } 
}
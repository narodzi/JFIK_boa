import java.util.Stack;


class LLVMGenerator {

   static String header_text = "";
   static String main_text = "";
   static int reg = 1;
   static int main_reg = 1;
   static String buffer = "";
   static int str = 1;
   static int br = 0;
   static Stack<Integer> br_stack = new Stack<Integer>();

   //structures
   static void declare_i32_struct_variable() {
      header_text += " i32,";
   }

   static void declare_double_struct_variable() {
      header_text += " double,";
   }

   static void declare_boolean_struct_variable() {
      header_text += " i1,";
   }

   static void begin_struct_declaration(String name) {
      header_text += "%" + name + " = type {";
   }

   static void end_struct_declaration() {
      header_text = header_text.substring(0, header_text.length()-1);
      header_text += " }\n";
   }

   static void define_structure(String name, String structureType) {
      buffer += "%" + name + " = alloca %" + structureType + "\n";
   }

   static void assign_value_to_structure_variable(String structTypeName, String structName, String variableName, int varIndex) {
      buffer +=  variableName + " = getelementptr %" + structTypeName + ", %" + structTypeName + "* " + structName + ", i32 0, i32 " + varIndex + "\n";
   }

   //structures end


   static void printf_i32(String id){
       buffer += "%"+reg+" = load i32, i32* "+id+"\n";
       reg++;
       buffer += "%"+reg+" = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @strpi, i32 0, i32 0), i32 %"+(reg-1)+")\n";
       reg++;
    }

    static void printf_double(String id){
       buffer += "%"+reg+" = load double, double* "+id+"\n";
       reg++;
       buffer += "%"+reg+" = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @strpd, i32 0, i32 0), double %"+(reg-1)+")\n";
       reg++;
    }

    static void printf_string(String id){
       buffer += "%"+reg+" = load i8*, i8** "+id+"\n";
       reg++;      
       buffer += "%"+reg+" = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @strps, i32 0, i32 0), i8* %"+(reg-1)+")\n";
       reg++;
    }

    static void printf_boolean(String id){
      buffer += "%" + reg + " = load i1, i1* " + id + "\n";
      reg++;
      buffer += "call void @print_bool(i1 %" + (reg-1) + ")\n"; 
   }

    static void scanf_i32(String id){
       buffer += "%"+reg+" = call i32 (i8*, ...) @scanf(i8* getelementptr inbounds ([3 x i8], [3 x i8]* @strs, i32 0, i32 0), i32* "+id+")\n";
       reg++;      
    }

    static void scanf_double(String id){
       buffer += "%"+reg+" = call i32 (i8*, ...) @scanf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @strsd, i32 0, i32 0), double* "+id+")\n";
       reg++;     
    }

    static void scanf_boolean(String id){
      main_text += "%"+reg+" = call i32 (i8*, ...) @scanf(i8* getelementptr inbounds ([3 x i8], [3 x i8]* @strs, i32 0, i32 0), i1* %"+id+")\n";
      reg++;     
   }

    static void scanf_string(String id, int l){
       allocate_string("str"+str, l);
       buffer += ""+id+" = alloca i8*\n";
       buffer += "%"+reg+" = getelementptr inbounds ["+(l+1)+" x i8], ["+(l+1)+" x i8]* %str"+str+", i64 0, i64 0\n";
       reg++;
       buffer += "store i8* %"+(reg-1)+", i8** "+id+"\n"; 
       str++;
       buffer += "%"+reg+" = call i32 (i8*, ...) @scanf(i8* getelementptr inbounds ([3 x i8], [3 x i8]* @strsi, i32 0, i32 0), i8* %"+(reg-1)+")\n";
       reg++;
    }

    static void declare_i32(String id){
       buffer += id+" = alloca i32\n";
    }
 
    static void declare_double(String id){
       buffer += id+" = alloca double\n";
    }

    static void declare_global_i32(String id) {
      header_text += id + " = global i32 0\n";
  }

  static void declare_global_double(String id) {
      header_text +=  id + " = global double 0.0\n";
  }

  static void declare_global_boolean(String id) {
      header_text += id + " = global i1 false\n";
   }

   static void declare_global_string(String id) {
      header_text += id + " = global i8* null\n";
   }

    static void declare_string(String id){
       buffer += id+" = alloca i8*\n";
    }

    static void allocate_string(String id, int l){
       buffer += "%"+id+" = alloca ["+(l+1)+" x i8]\n";
    }

    static void declare_boolean(String id) {
      buffer +=  id + " = alloca i1\n";
   }

    static void assign_i32(String id, String value){
       buffer += "store i32 "+value+", i32* "+id+"\n";
    }
 
    static void assign_double(String id, String value){
       buffer += "store double "+value+", double* "+id+"\n";
    }

    static void assign_string(String id){  
       buffer += "store i8* %"+(reg-1)+", i8** "+id+"\n";
    }

    static void assign_boolean(String id, String value) {
      buffer += "store i1 " + value + ", i1* " + id + "\n";
   }


    static void constant_string(String content){
       int l = content.length()+1;     
       header_text += "@str"+str+" = constant ["+l+" x i8] c\""+content+"\\00\"\n";
       String n = "str"+str;
       LLVMGenerator.allocate_string(n, (l-1));
       buffer += "%"+reg+" = bitcast ["+l+" x i8]* %"+n+" to i8*\n";
       buffer += "call void @llvm.memcpy.p0i8.p0i8.i64(i8* align 1 %"+reg+", i8* align 1 getelementptr inbounds (["+l+" x i8], ["+l+" x i8]* @"+n+", i32 0, i32 0), i64 "+l+", i1 false)\n";
       reg++;
       buffer += "%ptr"+n+" = alloca i8*\n";
       buffer += "%"+reg+" = getelementptr inbounds ["+l+" x i8], ["+l+" x i8]* %"+n+", i64 0, i64 0\n";
       reg++;
       buffer += "store i8* %"+(reg-1)+", i8** %ptr"+n+"\n";    
       str++;
    }

    static void load_i32(String id){
       buffer += "%"+reg+" = load i32, i32* "+id+"\n";
       reg++;
    }
 
    static void load_double(String id){
       buffer += "%"+reg+" = load double, double* "+id+"\n";
       reg++;
    }

    static void load_boolean(String id) {
      buffer += "%" + reg + " = load i1, i1* " + id + "\n";
      reg++;
   }

    static void load_string(String id){
       buffer += "%"+reg+" = load i8*, i8** "+id+"\n";
       reg++;
    }

    static void string_pointer(String id, int l){
       buffer += "%"+reg+" = getelementptr inbounds ["+(l+1)+" x i8], ["+(l+1)+" x i8]* %"+id+", i64 0, i64 0\n";
       reg++;
    }
 
    static void add_i32(String val1, String val2){
       buffer += "%"+reg+" = add i32 "+val1+", "+val2+"\n";
       reg++;
    }
 
    static void add_double(String val1, String val2){
       buffer += "%"+reg+" = fadd double "+val1+", "+val2+"\n";
       reg++;
    }

    static void sub_i32(String val1, String val2) {
       buffer += "%"+reg+" = sub i32 "+val1+", "+val2+"\n";
       reg++;
    }

    static void sub_double(String val1, String val2) {
       buffer += "%"+reg+" = fsub double "+val1+", "+val2+"\n";
       reg++;
    }
 
    static void mult_i32(String val1, String val2){
       buffer += "%"+reg+" = mul i32 "+val1+", "+val2+"\n";
       reg++;
    }
 
    static void mult_double(String val1, String val2){
       buffer += "%"+reg+" = fmul double "+val1+", "+val2+"\n";
       reg++;
    }

    static void div_i32(String val1, String val2){
       buffer += "%"+reg+" = sdiv i32 "+val1+", "+val2+"\n";
       reg++;
    }

    static void div_double(String val1, String val2){
       buffer += "%"+reg+" = fdiv double "+val1+", "+val2+"\n";
       reg++;
    }
 
    static void and_boolean(String val1, String val2) {
      buffer += "%" + reg + " = and i1 " + val1 + ", " + val2 + "\n";
      reg++;
   }

   static void or_boolean(String val1, String val2) {
      buffer += "%" + reg + " = or i1 " + val1 + ", " + val2 + "\n";
      reg++;
   }

   static void xor_boolean(String val1, String val2) {
      buffer += "%" + reg + " = xor i1 " + val1 + ", " + val2 + "\n";
      reg++;
   }

   static void neg_boolean(String val) {
      buffer += "%" + reg + " = xor i1 " + val + ", true \n";
      reg++;
   }

   static void assign_true(){
      buffer += "%" + reg + " = and i1 true, true\n";
      reg++;
   }

   static void assign_false(){
      buffer += "%" + reg + " = and i1 false, false\n";
      reg++;
   }

    static void sitofp(String id){
       buffer += "%"+reg+" = sitofp i32 "+id+" to double\n";
       reg++;
    }
 
    static void fptosi(String id){
       buffer += "%"+reg+" = fptosi double "+id+" to i32\n";
       reg++;
    }

    static void repeatStart(String repetitions){
      declare_i32("%" + Integer.toString(reg));
      int counter = reg;
      reg++;
      assign_i32("%" + Integer.toString(counter), "0");
      br++;
      buffer += "br label %cond"+br+"\n";
      buffer += "cond"+br+":\n";

      load_i32("%" + Integer.toString(counter));
      add_i32("%"+(reg-1), "1");
      assign_i32("%" + Integer.toString(counter), "%"+(reg-1));

      buffer += "%"+reg+" = icmp slt i32 %"+(reg-2)+", "+repetitions+"\n";
      reg++;

      buffer += "br i1 %"+(reg-1)+", label %true"+br+", label %false"+br+"\n";
      buffer += "true"+br+":\n";
      br_stack.push(br);
   }



    static void repeatEnd(){
      int b = br_stack.pop();
      buffer += "br label %cond"+b+"\n";
      buffer += "false"+b+":\n";
    }


    static void icmp(String id, String value){
      buffer += "%"+reg+" = load i32, i32* "+id+"\n";
      reg++;
      buffer += "%"+reg+" = icmp eq i32 %"+(reg-1)+", "+value+"\n";
      reg++;
   }

   static void ifstart(){
         br++;
         buffer += "br i1 %"+(reg-1)+", label %true"+br+", label %false"+br+"\n";
         buffer += "true"+br+":\n";
         br_stack.push(br);
   }

   static void ifend(){
         int b = br_stack.pop();
         buffer += "br label %false"+b+"\n";
         buffer += "false"+b+":\n";
   }


   static void functionstart(String id) {
      main_text += buffer; 
      main_reg = reg; 
      buffer = "define i32 @" + id + "() nounwind {\n";
      reg = 1; 
  }

  static void functionend() {
      buffer += "ret i32 %" + (reg - 1) + "\n";
      buffer += "}\n";
      header_text += buffer; 
      buffer = ""; 
      reg = main_reg; 
  }

  static void call(String id) { 
   buffer += "%" + reg + " = call i32 @" + id + "()\n";
   reg++;
}

   static void close_main(){
      main_text += buffer;
   }

    static String generate(){
       String text = "";
       text += "declare i32 @printf(i8*, ...)\n";
       text += "declare i32 @scanf(i8*, ...)\n";
       text += "declare i32 @sprintf(i8*, i8*, ...)\n";
       text += "declare i8* @strcpy(i8*, i8*)\n";
       text += "declare i8* @strcat(i8*, i8*)\n";
       text += "declare i32 @atoi(i8*)\n";
       text += "declare void @llvm.memcpy.p0i8.p0i8.i64(i8* noalias nocapture writeonly, i8* noalias nocapture readonly, i64, i1 immarg)\n";
       text += "@strps = constant [4 x i8] c\"%s\\0A\\00\"\n";
       text += "@strpi = constant [4 x i8] c\"%d\\0A\\00\"\n";
       text += "@strpd = constant [4 x i8] c\"%f\\0A\\00\"\n";
       text += "@strs = constant [3 x i8] c\"%d\\00\"\n";
       text += "@strsi = constant [3 x i8] c\"%s\\00\"\n";
       text += "@strsd = constant [4 x i8] c\"%lf\\00\"\n";
       text += "@true_str = constant [6 x i8] c\"true\n\\00\"\n";
       text += "@false_str = constant [7 x i8] c\"false\n\\00\"\n";
       text += "define void @print_bool(i1 %bool_val) {\n" +
         "%cond = icmp ne i1 %bool_val, 0\n"+
         "br i1 %cond, label %true_label, label %false_label\n"+
         "true_label:\n"+
         "%true_ptr = getelementptr inbounds [6 x i8], [6 x i8]* @true_str, i32 0, i32 0\n"+
         "call i32 (i8*, ...) @printf(i8* %true_ptr)\n"+
         "ret void\n"+
         "false_label:\n"+
         "%false_ptr = getelementptr inbounds [7 x i8], [7 x i8]* @false_str, i32 0, i32 0\n"+
         "call i32 (i8*, ...) @printf(i8* %false_ptr)\n"+
         "ret void\n"+
         "}\n";
       text += header_text;
       text += "define i32 @main() nounwind{\n";
       text += main_text;
       text += "ret i32 0 }\n";
       return text;
    }
}
class LLVMGenerator {

   static String header_text = "";
   static String main_text = "";
   static int reg = 1;

   static void printf_i32(String id) {
      main_text += "%" + reg + " = load i32, i32* %" + id + "\n";
      reg++;
      main_text += "%" + reg
            + " = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @strpi, i32 0, i32 0), i32 %"
            + (reg - 1) + ")\n";
      reg++;
   }

   static void printf_double(String id) {
      main_text += "%" + reg + " = load double, double* %" + id + "\n";
      reg++;
      main_text += "%" + reg
            + " = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @strpd, i32 0, i32 0), double %"
            + (reg - 1) + ")\n";
      reg++;
   }

   static void printf_boolean(String id){
      main_text += "%" + reg + " = load i1, i1* %" + id + "\n";
      reg++;
      main_text += "%" + reg
            + " = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @strpi, i32 0, i32 0), i1 %"
            + (reg - 1) + ")\n";
      reg++;
   }

   static void scanf_i32(String id) {
      main_text += "%" + reg
            + " = call i32 (i8*, ...) @scanf(i8* getelementptr inbounds ([3 x i8], [3 x i8]* @strs, i32 0, i32 0), i32* %"
            + id + ")\n";
      reg++;
   }

   static void scanf_double(String id) {
      main_text += "%" + reg
            + " = call i32 (i8*, ...) @scanf(i8* getelementptr inbounds ([3 x i8], [3 x i8]* @strsd, i32 0, i32 0), double* %"
            + id + ")\n";
      reg++;
   }

   static void declare_i32(String id) {
      main_text += "%" + id + " = alloca i32\n";
   }

   static void declare_double(String id) {
      main_text += "%" + id + " = alloca double\n";
   }

   static void declare_boolean(String id) {
      main_text += "%" + id + " = alloca i1\n";
   }

   static void assign_i32(String id, String value) {
      main_text += "store i32 " + value + ", i32* %" + id + "\n";
   }

   static void assign_double(String id, String value) {
      main_text += "store double " + value + ", double* %" + id + "\n";
   }

   static void assing_boolean(String id, String value) {
      main_text += "store i1 " + value + ", i1* %" + id + "\n";
   }

   static void load_i32(String id) {
      main_text += "%" + reg + " = load i32, i32* %" + id + "\n";
      reg++;
   }

   static void load_double(String id) {
      main_text += "%" + reg + " = load double, double* %" + id + "\n";
      reg++;
   }

   static void load_boolean(String id) {
      main_text += "%" + reg + " = load i1, i1* %" + id + "\n";
      reg++;
   }

   static void add_i32(String val1, String val2) {
      main_text += "%" + reg + " = add i32 " + val1 + ", " + val2 + "\n";
      reg++;
   }

   static void add_double(String val1, String val2) {
      main_text += "%" + reg + " = fadd double " + val1 + ", " + val2 + "\n";
      reg++;
   }

   static void sub_i32(String val1, String val2) {
      main_text += "%" + reg + " = sub i32 " + val1 + ", " + val2 + "\n";
      reg++;
   }

   static void sub_double(String val1, String val2) {
      main_text += "%" + reg + " = fsub double " + val1 + ", " + val2 + "\n";
      reg++;
   }

   static void mult_i32(String val1, String val2) {
      main_text += "%" + reg + " = mul i32 " + val1 + ", " + val2 + "\n";
      reg++;
   }

   static void mult_double(String val1, String val2) {
      main_text += "%" + reg + " = fmul double " + val1 + ", " + val2 + "\n";
      reg++;
   }

   static void div_i32(String val1, String val2) {
      main_text += "%" + reg + " = sdiv i32 " + val1 + ", " + val2 + "\n";
      reg++;
   }

   static void div_double(String val1, String val2) {
      main_text += "%" + reg + " = fdiv double " + val1 + ", " + val2 + "\n";
      reg++;
   }

   static void and_boolean(String val1, String val2) {
      main_text += "%" + reg + " = and i1 " + val1 + ", " + val2 + "\n";
      reg++;
   }

   static void or_boolean(String val1, String val2) {
      main_text += "%" + reg + " = or i1 " + val1 + ", " + val2 + "\n";
      reg++;
   }

   static void xor_boolean(String val1, String val2) {
      main_text += "%" + reg + " = xor i1 " + val1 + ", " + val2 + "\n";
      reg++;
   }

   static void neg_boolean(String val) {
      main_text += "%" + reg + " = xor i1 " + val + ", true \n";
      reg++;
   }

   static void assign_true(){
      main_text += "%" + reg + " = and i1 true, true\n";
      reg++;
   }

   static void assign_false(){
      main_text += "%" + reg + " = and i1 false, false\n";
      reg++;
   }

   static void sitofp(String id) {
      main_text += "%" + reg + " = sitofp i32 " + id + " to double\n";
      reg++;
   }

   static void fptosi(String id) {
      main_text += "%" + reg + " = fptosi double " + id + " to i32\n";
      reg++;
   }

   static String generate() {
      String text = "";
      text += "declare i32 @printf(i8*, ...)\n";
      text += "declare i32 @scanf(i8*, ...)\n";
      text += "@strpi = constant [4 x i8] c\"%d\\0A\\00\"\n";
      text += "@strpd = constant [4 x i8] c\"%f\\0A\\00\"\n";
      text += "@strs = constant [3 x i8] c\"%d\\00\"\n";
      text += "@strsd = constant [4 x i8] c\"%lf\\00\"\n";
      text += header_text;
      text += "define i32 @main() nounwind{\n";
      text += main_text;
      text += "ret i32 0 }\n";
      return text;
   }
}
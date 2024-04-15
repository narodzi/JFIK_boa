import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

public class Main {
    public static void main(String[] args) throws Exception {

        CharStream input = CharStreams.fromFileName(args[0]);

        boaLexer lexer = new boaLexer(input);

        CommonTokenStream tokens = new CommonTokenStream(lexer);
        boaParser parser = new boaParser(tokens);

        ParseTree tree = parser.prog(); 

        //System.out.println(tree.toStringTree(parser));

        System.out.print(';'); // todo remove

        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(new LLVMActions(), tree);

    }
}

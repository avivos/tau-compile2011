package IC.SemanticAnalyser;

public class StringType extends Type {

    public StringType(int id) { 
        this.id = id;
    }
    @Override
    public String toString() {
       return "string";
    }
    
}

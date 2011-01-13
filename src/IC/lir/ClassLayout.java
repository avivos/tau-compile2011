package IC.lir;

import java.util.LinkedHashMap;
import java.util.Map;

import IC.AST.Field;
import IC.AST.Method;
public class ClassLayout {
        private String name; //class name
        private int methodOffset;
        private int fieldOffset;
        
        private Map<String,Integer> methodToOffset;
        private Map<String,Integer> fieldToOffset;
       
        public ClassLayout(String name, ClassLayout parent) { 
            super();
            this.methodToOffset = new LinkedHashMap<String,Integer>(parent.getMethodToOffset());
            this.fieldToOffset = new LinkedHashMap<String, Integer>(parent.getFieldToOffset());
            methodOffset = parent.getMethodOffset();
            fieldOffset = parent.getFieldOffset();
            this.name = name;
        }
        
        public ClassLayout(String name){
            super();
            this.methodToOffset = new LinkedHashMap<String,Integer>();
            this.fieldToOffset = new LinkedHashMap<String, Integer>();
            methodOffset = 0;
            fieldOffset = 1;
            this.name = name;
        }        
        
        public int getFieldOffset(String field) { 
            if (!fieldToOffset.containsKey(field)) { 
                return -1;
            }
            return fieldToOffset.get(field);
        }
        
        public int getMethodOffset(String method) { 
            if (!methodToOffset.containsKey(method)) { 
                return -1;
            }
            return methodToOffset.get(method);
        }

        public void addMethodOffset(String method) {
            if (methodToOffset.containsKey(method)) {
                methodToOffset.put(method,methodToOffset.get(method));
            }
            else { 
                methodToOffset.put(method,methodOffset++);
            }
        }
        
        public void addFieldOffset(String field) { 
            fieldToOffset.put(field,fieldOffset++);
        }

        public int getMethodOffset() {
            return methodOffset;
        }

        public void setMethodOffset(int methodOffset) {
            this.methodOffset = methodOffset;
        }

        public int getFieldOffset() {
            return fieldOffset;
        }

        public void setFieldOffset(int fieldOffset) {
            this.fieldOffset = fieldOffset;
        }

        public Map<String, Integer> getMethodToOffset() {
            return methodToOffset;
        }

        public void setMethodToOffset(Map<String, Integer> methodToOffset) {
            this.methodToOffset = methodToOffset;
        }

        public Map<String, Integer> getFieldToOffset() {
            return fieldToOffset;
        }

        public void setFieldToOffset(Map<String, Integer> fieldToOffset) {
            this.fieldToOffset = fieldToOffset;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
        
        public String toString() { 
            StringBuffer result = new StringBuffer(this.name + ":\n");
            result.append("Fields:\n");
            for (String field : fieldToOffset.keySet()) { 
                result.append("\t" + field + " - " + fieldToOffset.get(field) + "\n");
            }
            result.append("\n");
            result.append("Methods:\n");
            for (String method : methodToOffset.keySet()) { 
                result.append("\t" + method + " - " + methodToOffset.get(method)+ "\n");
            }
            
            return result.toString();
            
        }

}

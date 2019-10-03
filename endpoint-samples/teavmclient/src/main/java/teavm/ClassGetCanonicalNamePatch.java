package teavm;

import org.teavm.model.*;
import org.teavm.model.util.InstructionVariableMapper;
import org.teavm.model.util.ProgramUtils;

public class ClassGetCanonicalNamePatch implements ClassHolderTransformer {
    @Override
    public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {
        if (cls.getName().equals("java.lang.Class")) {
            MethodReader origGetCName = context.getHierarchy().getClassSource().get("teavm.ClassGetCanonicalNameHelper").getMethod(new MethodDescriptor("getCName", Class.class, String.class));
            System.out.println(origGetCName);
            MethodHolder getCanonicalName = new MethodHolder("getCanonicalName", ValueType.parse(String.class));//ModelUtils.copyMethod(origGetCName, true);
            Program program = ProgramUtils.copy(origGetCName.getProgram());

            InstructionVariableMapper mapper = new InstructionVariableMapper(var -> var.getIndex() == 1 ? program.variableAt(0) : var);

            for (BasicBlock block : program.getBasicBlocks()) {
                mapper.apply(block);
            }
            program.deleteVariable(1);
            program.pack();
            System.out.println(program);
            System.out.println(getCanonicalName);

            cls.addMethod(getCanonicalName);
        }
    }
}

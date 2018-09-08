package za.ac.sun.cs.coastal;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedWriter;
import java.io.FileWriter;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

public class Instrumenter {

    public static void instrument(String inputClassName, String outputClassName) throws Exception {
        // FileInputStream is = new FileInputStream(inputClassName);
        ClassReader cr = new ClassReader(inputClassName);
        final String className =  outputClassName.replace('.', '/');
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        ClassAdapter ca = new ClassAdapter(cw, cr.getClassName());
        Remapper remapper = new Remapper() {
            @Override
            public String map(String typeName) {
                if (typeName.equals(cr.getClassName())) {
                    return className;
                }

                return super.map(typeName);
            }

        };

        ClassRemapper adapter = new ClassRemapper(ca, remapper);
        cr.accept(adapter, ClassReader.EXPAND_FRAMES);
        FileOutputStream fos = new FileOutputStream("build/classes/java/main/" + className + ".class");
        fos.write(cw.toByteArray());
        fos.close();
        BufferedWriter out = new BufferedWriter(new FileWriter(".branches", false)); // Only instrumenting a single class so append set to false.
        out.write(cr.getClassName() + ": " + Data.getCounter() + "\n");
        out.flush();
        out.close();
    }
}

class ClassAdapter extends ClassVisitor implements Opcodes {
    private String className;

    public ClassAdapter(final ClassVisitor cv, String className) {
        super(ASM6, cv);
        this.className = className;
    }

    @Override
    public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature,
            final String[] exceptions) {
        MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
        return mv == null ? null : new MethodAdapter(access, desc, mv, className);
    }
}

class MethodAdapter extends LocalVariablesSorter implements Opcodes {
    private String className;

    public MethodAdapter(int access, String desc, final MethodVisitor mv, String className) {
        super(ASM6, access, desc, mv);
        this.className = className;
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        int branchNo = Data.getNextBranchNo();
        Data.incCounter();

        // Do the call
        mv.visitJumpInsn(opcode, label);
        // If the tuple of branches is not in the map add it
        mv.visitMethodInsn(INVOKESTATIC, "za/ac/sun/cs/coastal/Data", "getPrevious", "()Ljava/lang/String;", false);
        mv.visitLdcInsn(className + "_" + branchNo);
        mv.visitMethodInsn(INVOKESTATIC, "za/ac/sun/cs/coastal/Data", "addTuple",
                "(Ljava/lang/String;Ljava/lang/String;)V", false);
        // Set the previous branch to this branch
        mv.visitLdcInsn(className + "_" + branchNo);
        mv.visitMethodInsn(INVOKESTATIC, "za/ac/sun/cs/coastal/Data", "setPrevious", "(Ljava/lang/String;)V", false);

    }
}

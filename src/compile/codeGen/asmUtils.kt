package compile.codeGen

import n.Ty
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

//Signature null, exceptions empty
internal fun ClassWriter.makeMethod(name: String, descriptor: String, body: MethodVisitor.() -> Unit) {
	val access = if (name == "<init>") Opcodes.ACC_PUBLIC else Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL
	val m = visitMethod(access, name, descriptor, /*signature*/null, /*exceptions*/emptyArray())
	m.body()
	m.visitMaxs(0, 0)
	m.visitEnd()
}

//TODO: move to utils
internal fun MethodVisitor.getField(typeName: String, fieldName: String, fieldType: String) {
	visitFieldInsn(Opcodes.GETFIELD, typeName, fieldName, fieldType)
}

internal fun MethodVisitor.putField(typeName: String, fieldName: String, fieldType: String) {
	visitFieldInsn(Opcodes.PUTFIELD, typeName, fieldName, fieldType)
}

internal fun MethodVisitor.invokeVirtual(invokedType: String, methodName: String, descriptor: String) {
	visitMethodInsn(Opcodes.INVOKEVIRTUAL, invokedType, methodName, descriptor, /*isInterface*/false)
}

internal fun MethodVisitor.ldc(value: Any) {
	visitLdcInsn(value)
}

internal fun MethodVisitor.tyReturn(ty: Ty) {
	visitInsn(ty.returnOpcode())
}

internal fun MethodVisitor.areturn() {
	visitInsn(Opcodes.ARETURN)
}

internal fun MethodVisitor.voidReturn() {
	visitInsn(Opcodes.RETURN)
}

internal fun MethodVisitor.aload0() {
	visitVarInsn(Opcodes.ALOAD, 0)
}

internal fun MethodVisitor.aload1() {
	visitVarInsn(Opcodes.ALOAD, 1)
}

internal fun MethodVisitor.iload(index: Int) {
	visitVarInsn(Opcodes.ILOAD, index)
}

internal fun MethodVisitor.astore1() {
	visitVarInsn(Opcodes.ASTORE, 1)
}

internal fun MethodVisitor.dup() {
	visitInsn(Opcodes.DUP)
}

internal fun MethodVisitor.pop() {
	visitInsn(Opcodes.POP)
}

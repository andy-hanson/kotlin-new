package cli

import compile.codeGen.writeRtClasses
import testU.*
import u.*
import n.*

import org.objectweb.asm.*
import org.objectweb.asm.Type
import org.objectweb.asm.signature.SignatureWriter
import org.objectweb.asm.util.TraceClassVisitor
import java.io.PrintWriter
import java.lang.reflect.*
import kotlin.reflect.jvm.javaMethod

//import kotlin.reflect.*
//import kotlin.reflect.jvm.*

//TODO: clean up this file
//Then TODO: builtins
//A builtin fn will need to carry around the java.lang.Class and java.lang.reflect.Method.
//Then we write its code using `callStatic`

fun main(args: Array<String>) {
	//val m = TestU.compile(Path.from("main"))

	val myRt = Rt.builtin("Point", "x", Prim.Int, "y", Prim.Int)
	val loader = DynamicClassLoader()
	writeRtClasses(myRt, loader)
	printClass(myRt.concrete_bytes)
	val instance = myRt.construct(1, 2)
	println(instance)
	//TODO: define toString() inside writeRtClasses so I can look at it!!!

	//useDummy()


	//val cls = AClass.Companion::class.java
	//val method = cls.getDeclaredMethod("foo")
	//println(method) // public final void cli.AClass$Companion.foo()
	//println(method.modifiers and java.lang.reflect.Modifier.STATIC) // 0
	//println(::bar.javaClass.enclosingClass.getMethod("bar").modifiers and java.lang.reflect.Modifier.STATIC) // 8
}


//TODO:PRIVATE
//TODO: store it on the Rt as a Lazy
public fun dummyInterface(): ByteArray {
	val cw = ClassWriter(0)
	val name = "IAmAFace" //rt.origin.interface_name()
	val access = Opcodes.ACC_PUBLIC + Opcodes.ACC_ABSTRACT + Opcodes.ACC_INTERFACE
	val superType = "java/lang/Object"
	val superInterfaces = arrayOf<String>()
	cw.visit(Opcodes.V1_8, access, name, null, superType, superInterfaces)

	val descriptor = Type.getMethodDescriptor(Type.INT_TYPE)
	cw.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_ABSTRACT, "x", descriptor, /*signature*/null, /*exceptions*/emptyArray())

	cw.visitEnd()

	return cw.toByteArray()
}

public fun dummyImplementer(): ByteArray {
	val cw = ClassWriter(ClassWriter.COMPUTE_FRAMES)
	val name = "Concrete"
	val access = Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL
	val superType = "java/lang/Object"
	val superInterfaces = arrayOf("IAmAFace")
	cw.visit(Opcodes.V1_8, access, name, null, superType, superInterfaces)

	val intType = "I"
	cw.visitField(Opcodes.ACC_PUBLIC, "x", intType, /*signature*/null, /*value*/null)

	//TODO: take "x" as a parameter
	//TODO: missing something?
	val mvCtr = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE), /*signature*/null, /*exceptions*/emptyArray())
	mvCtr.visitVarInsn(Opcodes.ALOAD, 0)
	mvCtr.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", Type.getMethodDescriptor(Type.VOID_TYPE), /*isInterface*/false)
	mvCtr.visitVarInsn(Opcodes.ALOAD, 0)
	mvCtr.visitVarInsn(Opcodes.ILOAD, 1)
	mvCtr.visitFieldInsn(Opcodes.PUTFIELD, "Concrete", "x", intType)
	mvCtr.visitInsn(Opcodes.RETURN)
	mvCtr.visitMaxs(0, 0)
	mvCtr.visitEnd()

	val descriptor = Type.getMethodDescriptor(Type.INT_TYPE)
	val mv = cw.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL, "x", descriptor, /*signature*/null, /*exceptions*/emptyArray())
	//Return 1
	mv.visitInsn(Opcodes.ICONST_1)
	mv.visitInsn(Opcodes.IRETURN)
	mv.visitMaxs(0, 0)
	mv.visitEnd()

	cw.visitEnd()

	return cw.toByteArray()

}

fun useDummy() {
	val loader = DynamicClassLoader()
	val iface = loader.define("IAmAFace", dummyInterface())

	val cls = dummyImplementer()
	//printClass(cls)
	val impl = loader.define("Concrete", cls)
	val ctr = impl.getConstructor(Int::class.java)
	val instance = ctr.newInstance(0)
	val x = impl.getMethod("x").invoke(instance)
	println(x)
	//val impl = loader.define("Concrete", dummyImplementer())
	//instantiate it
	//val instance = impl.newInstance()
	//println(instance)
}




fun test() {
	/*
	val path = Path.of("main.nz")
	val s = TestU.lex(path)
	println(s)
	val ast = TestU.parse(path)
	val ss = ast.toSexpr()
	println(ss)
	*/
}


fun runMain(bytes: ByteArray) {
	val loader = DynamicClassLoader()
	val klass = loader.define("hello.HelloWorld", bytes)
	val method = klass.getMethod("foo", MyClass::class.java)
	val result = method.invoke(null, MyClass(1))
	println(result)
}

fun printClass(bytes: ByteArray) {
	val reader = ClassReader(bytes)
	val visitor = TraceClassVisitor(PrintWriter(System.out))
	reader.accept(visitor, ClassReader.SKIP_DEBUG)
}





fun asmTest(): ByteArray {
	val cw = ClassWriter(ClassWriter.COMPUTE_FRAMES)
	cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER, "hello/HelloWorld", null, "java/lang/Object", null)
	cw.visitSource("HelloWorld.nz", null)

	writeFunc(cw)

	cw.visitEnd()
	return cw.toByteArray()
}

fun writeFunc(cw: ClassWriter) {
	val mv = cw.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, "foo", signatureString(), null, null)

	mv.visitVarInsn(Opcodes.ALOAD, 0)

	mv.callStatic(MyClass::class, MyClass::class.java.getMethod("incr", MyClass::class.java))

	mv.visitInsn(Opcodes.ARETURN)
	mv.visitMaxs(0, 0)
	mv.visitEnd()
}

fun MethodVisitor.callStatic(klass: kotlin.reflect.KClass<*>, called: Method) {
	val j = called

	val a = j.getDeclaringClass()
	val b = j.declaringClass
	assert(a === b)

	val k = j.declaringClass.javaClass
	assert(k === klass.java)
	println(a === b)
	println(k === klass.java)
	val desc = Type.getMethodDescriptor(j)
	visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(k), called.name, desc, /*isInterface*/ false)
}

fun signatureString(): String {
	val w = SignatureWriter()

	w.visitParameterType()
	sigType(w)

	w.visitReturnType()
	sigType(w)


	println(w.toString())
	return w.toString()
}

fun sigType(w: SignatureWriter) {
	w.visitClass(MyClass::class.java) // This also works for interfaces.
}

fun SignatureWriter.visitClass(klass: Class<*>) {
	//visitBaseType('Z')
	visitClassType(Type.getInternalName(klass))
	//visitClassType(klass.java.canonicalName)
	visitEnd()
}

data class MyClass(val i: Int) {
	companion object {
		@JvmStatic
		fun incr(a: MyClass): MyClass =
			MyClass(a.i + 1)
	}
}

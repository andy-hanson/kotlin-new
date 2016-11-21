package compile.codeGen

import cli.DynamicClassLoader
import n.Rt
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type


private val VERSION = Opcodes.V1_8
private val OBJECT = "java/lang/Object"

fun writeRtClasses(rt: Rt, loader: DynamicClassLoader) {
	if (rt.origin is Rt.Origin.GenInst)
		TODO("Generic Rt needs 'signature' in various places...")

	val iface: ByteArray = makeInterface(rt)
	rt.iface = loader.define(rt.javaTypeName(), iface)

	val concrete = makeConcreteClass(rt)
	rt.concrete = loader.define(rt.concreteTypeName(), concrete)
	rt.concrete_bytes = concrete
}

private fun makeInterface(rt: Rt): ByteArray =
	ClassWriter(0).run {
		val access = Opcodes.ACC_PUBLIC + Opcodes.ACC_ABSTRACT + Opcodes.ACC_INTERFACE
		visit(VERSION, access, rt.javaTypeName(), null, /*superClass*/OBJECT, /*superInterfaces*/emptyArray())

		for (prop in rt.properties) {
			visitMethod(
				Opcodes.ACC_PUBLIC + Opcodes.ACC_ABSTRACT,
				prop.nameStr,
				"()${prop.javaType}",
				/*signature*/null,
				/*exceptions*/emptyArray())
		}

		visitEnd()
		toByteArray()
	}

private fun makeConcreteClass(rt: Rt): ByteArray =
	ClassWriter(ClassWriter.COMPUTE_FRAMES).run {
		//TODO: generic Rt will need a signature...
		visit(VERSION, Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL, rt.concreteTypeName(), /*signature*/null, /*superClass*/OBJECT, /*superInterfaces*/emptyArray())
		makeFields(rt)
		makeConstructor(rt)
		makeToString(rt)
		visitEnd()
		toByteArray()
	}

fun ClassWriter.makeConstructor(rt: Rt) {
	val ctrDescriptor = "(" + rt.properties.map { p -> p.javaType }.joinToString("") + ")V"//TODO: use Type method
	makeMethod("<init>", ctrDescriptor) {
		aload0()
		visitMethodInsn(Opcodes.INVOKESPECIAL, OBJECT, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE), /*isInterface*/false)
		for ((propIndex, prop) in rt.properties.withIndex()) {
			aload0()
			iload(propIndex + 1) //TODO: propType.loadOpcode(), and use index
			putField(rt.concreteTypeName(), prop.nameStr, prop.javaType)
		}
		voidReturn()
	}
}

private fun ClassWriter.makeFields(rt: Rt) {
	for (prop in rt.properties) {
		// ("value" is only for static fields)
		visitField(Opcodes.ACC_PUBLIC, prop.nameStr, prop.javaType, /*signature*/null, /*value*/null)
		makeFieldGetter(rt, prop)
	}
}

private fun ClassWriter.makeFieldGetter(rt: Rt, prop: Rt.Property) {
	makeMethod(prop.nameStr, "()${prop.javaType}") { //TODO: use Type helper
		aload0()
		getField(rt.concreteTypeName(), prop.nameStr, prop.javaType)
		tyReturn(prop.ty)
	}
}

private fun ClassWriter.makeToString(rt: Rt) {
	val rtName = rt.javaTypeName()
	val stringBuilderType = "java/lang/StringBuilder"
	val stringBuilderTypeType = Type.getType(StringBuilder::class.java)
	val stringType = Type.getType(String::class.java)
	makeMethod("toString", Type.getMethodDescriptor(stringType)) {
		visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder")
		dup()
		ldc(rtName + "(")
		visitMethodInsn(Opcodes.INVOKESPECIAL, stringBuilderType, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, stringType), /*isInterface*/false)
		astore1()
		for ((propIndex, prop) in rt.properties.withIndex()) {
			val (propName, j) = Pair(prop.nameStr, prop.javaType)
			aload1()
			aload0()
			getField(rt.concreteTypeName(), propName, j)
			invokeVirtual(stringBuilderType, "append", Type.getMethodDescriptor(stringBuilderTypeType, Type.getType(j)))
			pop()
			if (propIndex != rt.properties.size - 1) {
				aload1()
				ldc(", ")
				invokeVirtual(stringBuilderType, "append", Type.getMethodDescriptor(stringBuilderTypeType, stringType))
				pop()
			}
		}
		aload1()
		ldc(")")
		invokeVirtual(stringBuilderType, "append", Type.getMethodDescriptor(stringBuilderTypeType, stringType))
		// Don't bother popping, return it!
		//tos.visitVarInsn(Opcodes.ALOAD, 1)
		invokeVirtual(stringBuilderType, "toString", Type.getMethodDescriptor(stringType))
		areturn()
	}
}

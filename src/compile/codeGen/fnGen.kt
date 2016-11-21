package compile.codeGen

import n.*
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Type

interface NzFn0<out T> {
	fun call(): T
}
interface NzFn1<in T, out U> {
	fun call(t: T): U
}
interface NzFn2<in T, in U, out V> {
	fun call(t: T, u: U): V
}


fun fnStuff(fn: Fn.Declared) {
	fn.origin
	fn.ty
	fn.parameters
	fn.body
	ClassWriter(ClassWriter.COMPUTE_FRAMES).apply {
		val ft = (fn.ty as FtOrGen.F).ft
		makeMethod("call", ft.descriptor()) {
			writeBody(fn.body)
		}
	}
}

private fun MethodVisitor.writeBody(body: Expr) {
	return when (body) {
		is LocalDeclare -> {
			TODO()
		}
		is LocalAccess -> {
			TODO()
		}
		is Convert -> {
			TODO()
		}
		is Value -> {
			TODO()
		}
		is Call -> {
			TODO()
		}
		is Cs -> {
			TODO()
		}
		is Ts -> {
			TODO()
		}
		is GetProperty -> {
			TODO()
		}
		is Let -> {
			TODO()
		}
		is Seq -> {
			TODO()
		}
		is Partial -> {
			TODO()
		}
		is Quote -> {
			TODO()
		}
		is Check -> {
			TODO()
		}
		is EList -> {
			TODO()
		}
		is Lambda -> {
			TODO()
		}
	}
}


fun Ft.descriptor(): String =
	Type.getMethodDescriptor(
		Type.getType(signature.returnTy.javaTypeName()),
		*signature.parameters.map { p -> Type.getType(p.ty.javaTypeName()) }.toArray())

//Code for a function is a class with a static INSTANCE.
//Code for a lambda is a class with a constructor.
//In either case it should implement the appropriate interface here: https://docs.oracle.com/javase/8/docs/api/java/util/function/package-summary.html
//Nah, just implement NzFn0, NzFn1, etc
class Code(val cls: Class<*>) {

}

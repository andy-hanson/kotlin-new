package n

import compile.codeGen.Code
import compile.partiallyApplyFt
import u.*
import java.lang.reflect.*

sealed class V {
	sealed class Prim : V() {
		class Bool(val value: Boolean) : V()
		class Float(val value: Double) : V()
		class Int(val value: Long) : V()
		class String(val value: kotlin.String) : V()
		object Void

		class List(val elements: Arr<V>) : V()
	}
}

/** This stores the rt for debugging purposes only. One day we should just store properties. */
class Rc(val ty: Rt, val properties: Arr<V>)
/** This stores the vt for debugging purposes only. */
class Vv(val ty: Vt, val tag: Int, val value: V)

sealed class Fn : V() {
	abstract val ty: FtOrGen

	/** A function declared as code in a module. */
	class Declared(val origin: CodeOrigin, override val ty: FtOrGen) : Fn() {
		var parameters: Arr<LocalDeclare> by Late()
		var body: Expr by Late()
		var code: Code by Late()
	}

	/**
	The backing function for a lambda expression.
	Actual lambda instances will be partial applications of this.
	*/
	class Lambda(
		val origin: Loc,
		val explicitParameters: Arr<LocalDeclare>,
		val closureParameters: Arr<LocalDeclare>,
		val body: Expr,
		val ft: Ft) : Fn()  {

		override val ty: FtOrGen
			get() = FtOrGen.F(ft)

		var code: Code by Late()
	}

	/** A builtin function. */
	class Builtin(val name: Sym, override val ty: FtOrGen, val method: Method) : Fn() {
		init {
			checkModifiers(method.modifiers)
		}

		constructor(name: String, ft: Ft, method: Method) : this(name.sym, FtOrGen.F(ft), method) {}

		companion object {
			operator fun invoke(name: String, returnTy: Ty, parameters: Arr<Pair<String, Ty>>, method: Method): Builtin {
				val ft = Ft()
				val fn = Builtin(name.sym, FtOrGen.F(ft), method)
				ft.origin = Ft.Origin.FromBuiltinFn(fn)
				ft.signature = Ft.Signature.builtin(returnTy, parameters)
				return fn
			}

			private fun generic(name: String, genParamNames: Arr<String>, makeSignature: (Arr<Ty>) -> Pair<Ty, Arr<Pair<String, Ty>>>, method: Method): Builtin {
				val genParams = genParamNames.map { GenVar.Builtin(it.sym) }
				val g = GenFt(genParams)
				val (returnTy, parameters) = makeSignature(genParams)
				g.signature = Ft.Signature.builtin(returnTy, parameters)
				val f = Builtin(name.sym, FtOrGen.G(g), method)
				g.origin = GenFt.Origin.FromBuiltinFn(f)
				return f
			}

			fun generic(name: String, genParamName: String, makeSignature: (Ty) -> Pair<Ty, Arr<Pair<String, Ty>>>, method: Method) =
				generic(name, Arr.of(genParamName), { makeSignature(it.single()) }, method)
		}


		private fun checkModifiers(modifiers: Int) {
			assert(Modifier.isFinal(modifiers))
			val no = listOf(Modifier.ABSTRACT, Modifier.INTERFACE, Modifier.NATIVE, Modifier.PRIVATE, Modifier.PROTECTED, Modifier.SYNCHRONIZED, Modifier.TRANSIENT, Modifier.VOLATILE)
			for (flag in no)
				forbid(modifiers hasFlag flag)
			assert(Modifier.isPublic(modifiers))
			assert(Modifier.isStatic(modifiers)) // TODO: allow instance methods
		}
	}

	/** Partial application of any other function. */
	class Partial(val partiallyApplied: Fn, val partialArgs: Arr<V>) : Fn() {
		override val ty: FtOrGen
			get() = FtOrGen.F(partiallyApplyFt((partiallyApplied.ty as FtOrGen.F).ft, partialArgs.size))
	}

	class Ctr(val rt: Rt) : Fn() {
		//TODO: store the ty here upon creation. And create the Ctr upon creating the rt.
		override val ty: FtOrGen
			get() = TODO()
	}

	class Instance(val instantiated: Fn, val tyArgs: Arr<Ty>, val ft: Ft) : Fn() {
		override val ty: FtOrGen
			get() = FtOrGen.F(ft)
	}
}

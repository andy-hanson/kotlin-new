package n

import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

import u.*
import java.util.HashMap

/**
Origin for something that was explicitly written down
(as opposed to builtin or created from generic application)
*/
class CodeOrigin(val module: Module, val loc: Loc, val name: Sym)

sealed class TyOrGen

/** A concrete type (including an instantiation of a generic type. */
sealed class Ty : TyOrGen() {
	// This is available *early*
	abstract fun javaTypeName(): String
	// This is available after codegen.
	abstract fun javaType(): Class<*>

	open fun returnOpcode(): Int {
		return Opcodes.ARETURN
	}
}

sealed class Prim(nameStr: String) : Ty() {
	val name: Sym = nameStr.sym

	object Bool : Prim("Bool") {
		override fun javaTypeName() = "Z"
		override fun javaType() = java.lang.Boolean.TYPE
		override fun returnOpcode() = Opcodes.IRETURN
	}
	object Float : Prim("Float") {
		override fun javaTypeName() = "D"
		override fun javaType() = java.lang.Double.TYPE
		override fun returnOpcode() = Opcodes.DRETURN
	}
	object Int : Prim("Int") {
		override fun javaTypeName() = "I"
		override fun javaType() = java.lang.Integer.TYPE
		override fun returnOpcode() = Opcodes.IRETURN
	}
	object Str : Prim("String") {
		override fun javaTypeName() = "java/lang/String"
		override fun javaType() = String::class.java
	}
	object Void : Prim("Void") {
		override fun javaTypeName() = "V"
		// Void should never be used as a parameter type.
		override fun javaType() = throw NotImplementedError()
		override fun returnOpcode() = Opcodes.RETURN
	}
	/** Dummy value. There is no 'nil' type. */
	object Nil : Prim("Nil") {
		override fun javaTypeName() = throw NotImplementedError()
		override fun javaType() = throw NotImplementedError()
	}
}


interface GenInstOrigin {
	val gen: Gen<*>
	val instWith: Arr<Ty>
}

/**
Instance of a GenPrim.
Do not directly construct this. Instead use instantiate_gen_prim.
*/
class PrimInst(override val gen: GenPrim, override val instWith: Arr<Ty>) : Ty(), GenInstOrigin {
	override fun javaTypeName() = TODO()
	override fun javaType() = TODO()
}

class Rt(val origin: Origin) : Ty() {
	constructor(origin: Origin, properties: Arr<Property>) : this(origin) { this.properties = properties }

	override fun javaTypeName() = origin.interface_name()
	fun concreteTypeName() = javaTypeName() + "_Concrete"
	override fun javaType() = iface

	var properties: Arr<Property> by Late()

	// Interface for when this is used as a type.
	var iface: Class<*> by Late()
	// Class for when this is used as a constructor.
	var concrete: Class<*> by Late()
	//TODO: this is just for debugging...
	var concrete_bytes: ByteArray by Late()

	fun construct(vararg args: Any): Any {
		val parameterTypes = properties.map { prop ->
			prop.ty.javaType()
		}
		val ctr = concrete.getConstructor(*parameterTypes.toArray())
		println(listOf(*args))
		return ctr.newInstance(*args)
	}


	companion object {
		private fun builtin(name: String, props: Arr<Pair<String, Ty>>): Rt =
			Rt(Origin.Builtin(name.sym), props.map { Property(it.first.sym, it.second) })

		fun builtin(name: String, prop0Name: String, prop0Ty: Ty): Rt =
			builtin(name, Arr.of(prop0Name to prop0Ty))

		fun builtin(name: String, prop0Name: String, prop0Ty: Ty, prop1Name: String, prop1Ty: Ty): Rt =
			builtin(name, Arr.of(prop0Name to prop0Ty, prop1Name to prop1Ty))
	}

	sealed class Origin {
		abstract fun interface_name(): String

		//TODO: Rt, Vt, Ft have these two origins in common.
		class Builtin(val name: Sym) : Origin() {
			override fun interface_name() = name.str
		}
		class Decl(val origin: CodeOrigin) : Origin() {
			override fun interface_name() =
				origin.name.str
		}
		data class GenInst(override val gen: GenRt, override val instWith: Arr<Ty>) : Origin(), GenInstOrigin {
			override fun interface_name() =
				throw NotImplementedError("Should never call this")
		}
	}

	class Property(val name: Sym, val ty: Ty) {
		val nameStr: String
			get() = name.str

		val javaType: String
			get() = ty.javaTypeName()
	}
}

class Vt(val origin: Origin) : Ty() {
	var variants: Arr<Ty> by Late()

	override fun javaTypeName() = "java/lang/Object" // Must be pattern-matched to do anything with it.
	override fun javaType() = Object::class.java

	sealed class Origin {
		class Builtin(val name: Sym) : Origin()
		class Decl(val origin: CodeOrigin) : Origin()
		class GenInst(override val gen: GenVt, override val instWith: Arr<Ty>) : Origin(), GenInstOrigin
	}
}

class Ft() : Ty() {
	override fun javaTypeName() = origin.javaTypeName()
	override fun javaType() = TODO()

	constructor(origin: Origin) : this() {
		this.origin = origin
	}
	constructor(origin: Origin, signature: Signature) : this(origin) {
		this.signature = signature
	}
	companion object {
		private fun builtin(name: String, returnTy: Ty, parameters: Arr<Pair<String, Ty>>): Ft =
			Ft(Ft.Origin.Builtin(name.sym), Signature.builtin(returnTy, parameters))

		fun builtin(name: String, returnTy: Ty): Ft =
			builtin(name, returnTy, Arr.empty())

		fun builtin(name: String, returnTy: Ty, param0Name: String, param0Ty: Ty): Ft =
			builtin(name, returnTy, Arr.of(param0Name to param0Ty))

		fun builtin(name: String, returnTy: Ty, param0Name: String, param0Ty: Ty, param1Name: String, param1Ty: Ty): Ft =
			builtin(name, returnTy, Arr.of(param0Name to param0Ty, param1Name to param1Ty))
	}

	var origin: Origin by Late()
	var signature: Signature by Late()

	sealed class Origin {
		abstract fun javaTypeName(): String

		class Builtin(val name: Sym) : Origin() {
			override fun javaTypeName() = name.str
		}
		class Decl(val origin: CodeOrigin) : Origin() {
			override fun javaTypeName() = origin.name.str
		}
		class Lambda(val loc: Loc) : Origin() {//TODO: what to store here?
			override fun javaTypeName() = TODO()
		}
		class FromDeclaredFn(val fn: Fn.Declared) : Origin() {
			override fun javaTypeName() = TODO()
		}
		class FromBuiltinFn(val fn: Fn.Builtin) : Origin() {
			override fun javaTypeName() = TODO()
		}
		class FromRt(val rt: Rt) : Origin() {
			override fun javaTypeName() = TODO()
		}
		class FromPartial(val partiallyApplied: Ft) : Origin() {
			override fun javaTypeName() = TODO()
		}
		class GenInst(override val gen: GenFt, override val instWith: Arr<Ty>) : Origin(), GenInstOrigin {
			override fun javaTypeName() = TODO()
		}
	}

	class Parameter(val name: Sym, val ty: Ty)
	data class Signature(val returnTy: Ty, val parameters: Arr<Parameter>) {
		companion object {
			fun builtin(returnTy: Ty, parameters: Arr<Pair<String, Ty>>) =
				Signature(returnTy, parameters.map { Parameter(it.first.sym, it.second) })
		}
	}
}

// A parameter of a Gen.
sealed class GenVar : Ty() {
	override fun javaType() = TODO() //???

	class Builtin(val name: Sym) : GenVar() {
		override fun javaTypeName() = name.str
	}
	class Declared(val origin: CodeOrigin) : GenVar() {
		override fun javaTypeName() = origin.name.str
	}
}

sealed class Gen<Concrete>(val tyParams: Arr<GenVar>) : TyOrGen() {
	val cache = HashMap<Arr<Ty>, Concrete>()
}

/** Generic primitive. */
sealed class GenPrim(params: Arr<GenVar>) : Gen<PrimInst>(params) {
	object List : GenPrim(Arr.of(GenVar.Builtin("Element".sym)))

}

class GenRt(val origin: CodeOrigin, params: Arr<GenVar>) : Gen<Rt>(params) {
	var properties: Arr<Rt.Property> by Late()
}

class GenVt(val origin: CodeOrigin, params: Arr<GenVar>) : Gen<Vt>(params) {
	var variants: Arr<Ty> by Late()
}

class GenFt(params: Arr<GenVar>) : Gen<Ft>(params) {
	constructor(params: Arr<GenVar>, origin: Origin) : this(params) {
		this.origin = origin
	}
	constructor(params: Arr<GenVar>, origin: Origin, signature: Ft.Signature): this(params, origin) {
		this.signature = signature
	}

	companion object {
		fun builtin(name: String, genParamNames: Arr<String>, makeSignature: (Arr<Ty>) -> Pair<Ty, Arr<Pair<String, Ty>>>): GenFt =
			builtinWithoutOrigin(genParamNames, makeSignature).apply { this.origin = Origin.Builtin(name.sym) }

		fun builtin(name: String, genParamName: String, makeSignature: (Ty) -> Pair<Ty, Arr<Pair<String, Ty>>>): GenFt =
			builtin(name, Arr.of(genParamName)) { makeSignature(it.single()) }

		// Used for constructing Py.
		fun builtinWithoutOrigin(genParamNames: Arr<String>, makeSignature: (Arr<Ty>) -> Pair<Ty, Arr<Pair<String, Ty>>>): GenFt {
			val genParams = genParamNames.map { GenVar.Builtin(it.sym) }
			val (returnTy, parameters) = makeSignature(genParams)
			val signature = Ft.Signature(returnTy, parameters.map { Ft.Parameter(it.first.sym, it.second)})
			return GenFt(genParams).apply { this.signature = signature }
		}
	}

	var origin: Origin by Late()
	var signature: Ft.Signature by Late()

	sealed class Origin {
		class FromPy(val py: Py) : Origin()
		class Builtin(val name: Sym) : Origin()
		class Declared(val origin: CodeOrigin) : Origin()
		class FromDeclaredFn(val fn: Fn.Declared) : Origin()
		class FromBuiltinFn(val fn: Fn.Builtin) : Origin()
		class FromGenRt(val genRt: GenRt) : Origin()
	}
}



//TODO: would like to use a sealed interface...
sealed class FtOrGen {
	abstract fun toTyOrGen(): TyOrGen
	abstract val signature: Ft.Signature

	class F(val ft: Ft) : FtOrGen() {
		override fun toTyOrGen() = ft
		override val signature: Ft.Signature
			get() = ft.signature
	}
	class G(val genFt: GenFt) : FtOrGen() {
		override fun toTyOrGen() = genFt
		override val signature: Ft.Signature
			get() = genFt.signature
	}
}


//TODO: TyUU stuff

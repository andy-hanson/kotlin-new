package cli

class DynamicClassLoader : ClassLoader {
	constructor() : super()
	constructor(parent: ClassLoader) : super(parent)

	fun define(className: String, bytecode: ByteArray): Class<out Any> =
		super.defineClass(className, bytecode, 0, bytecode.size)
}
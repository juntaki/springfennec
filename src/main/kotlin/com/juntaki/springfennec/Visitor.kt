package com.juntaki.springfennec

import io.swagger.annotations.ApiOperation
import io.swagger.models.Operation
import io.swagger.models.Path
import io.swagger.models.Swagger
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import javax.lang.model.element.*
import javax.lang.model.util.ElementScanner8
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

class Visitor(
        private val swagger: Swagger,
        private val elementUtils: Elements,
        private val typeUtils: Types
) : ElementScanner8<Void?, Void?>() {
    val context = Context()
    // Class
    override fun visitType(e: TypeElement?, p: Void?): Void? {
        val requestMapping: RequestMapping? = e!!.getAnnotation(RequestMapping::class.java)
        requestMapping?.let {
            // Add base mapping
        }
        return super.visitType(e, p)
    }

    // Function
    override fun visitExecutable(e: ExecutableElement?, p: Void?): Void? {
        val requestMapping: RequestMapping? = e!!.getAnnotation(RequestMapping::class.java)
        requestMapping?.let {
            requestMapping.value.forEach {
                // path
                var path = swagger.getPath(context.currentPath + it)
                if (path == null) {
                    path = Path()
                }

                // one method is one operation
                val operation = Operation()

                // summary, nickname
                val apiOperation: ApiOperation? = e.getAnnotation(ApiOperation::class.java)
                apiOperation?.let {
                    operation.operationId = it.nickname
                    if (operation.operationId.isEmpty()) operation.operationId = "todo"
                    operation.summary = it.value
                    operation.tags = it.tags.toList()
                }

                e.accept(ParamVisitor(swagger, operation.parameters, elementUtils, typeUtils), null)
                requestMapping.method.forEach {
                    when(it) {
                        RequestMethod.DELETE -> path.delete = operation
                        RequestMethod.GET -> path.get = operation
                        RequestMethod.OPTIONS -> path.options = operation
                        RequestMethod.PATCH -> path.patch = operation
                        RequestMethod.POST -> path.post = operation
                        RequestMethod.PUT -> path.put = operation
                        RequestMethod.HEAD -> path.head = operation
                        RequestMethod.TRACE -> TODO("Not implemented")
                    }
                }
                swagger.path(context.currentPath + it ,path)
            }
        }
        return super.visitExecutable(e, p)
    }


}
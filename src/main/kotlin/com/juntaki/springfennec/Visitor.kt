package com.juntaki.springfennec

import io.swagger.models.Swagger
import org.springframework.web.bind.annotation.RequestMapping
import javax.lang.model.element.*
import javax.lang.model.util.ElementScanner8

class Visitor(
        val swagger: Swagger
) : ElementScanner8<String?, Swagger?>() {
    val context = Context()
    // Class
    override fun visitType(e: TypeElement?, p: Swagger?): String? {
        val requestMapping: RequestMapping? = e!!.getAnnotation(RequestMapping::class.java)
        requestMapping?.let {
            println(requestMapping)
        }
        return super.visitType(e, p)
    }

    // Function
    override fun visitExecutable(e: ExecutableElement?, p: Swagger?): String? {
        e!!.annotationMirrors.forEach {
            println(it)
        }
        val requestMapping: RequestMapping? = e!!.getAnnotation(RequestMapping::class.java)
        requestMapping?.let {
            println(requestMapping)
        }
        return super.visitExecutable(e, p)
    }

    // Parameter
    override fun visitVariable(e: VariableElement?, p: Swagger?): String? {
        e!!.annotationMirrors.forEach {
        }
        return super.visitVariable(e, p)
    }
}
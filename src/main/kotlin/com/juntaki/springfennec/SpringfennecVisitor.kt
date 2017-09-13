package com.juntaki.springfennec

import io.swagger.models.Swagger
import javax.lang.model.element.*
import javax.lang.model.util.ElementScanner8

class SpringfennecVisitor : ElementScanner8<String?, Swagger?>() {
    // Api
    override fun visitType(e: TypeElement?, p: Swagger?): String? {
        e!!.annotationMirrors.forEach {
            println(it)
        }
        return super.visitType(e, p)
    }

    // ApiOperation
    override fun visitExecutable(e: ExecutableElement?, p: Swagger?): String? {
        e!!.annotationMirrors.forEach {
            println(it)
        }
        return super.visitExecutable(e, p)
    }

    // ApiParam
    override fun visitVariable(e: VariableElement?, p: Swagger?): String? {
        e!!.annotationMirrors.forEach {
            println(it)
        }
        return super.visitVariable(e, p)
    }
}
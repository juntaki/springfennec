package com.juntaki.springfennec

import io.swagger.models.parameters.Parameter
import io.swagger.models.parameters.RefParameter
import javax.lang.model.element.VariableElement
import javax.lang.model.util.ElementScanner8

class ParamVisitor(
        private val parameters: MutableList<Parameter>
) : ElementScanner8<Void?, Void?>() {
    override fun visitVariable(e: VariableElement?, p: Void?): Void? {
        val param = RefParameter(e!!.asType().toString())
        parameters.add(param)
        return super.visitVariable(e, p)
    }
}
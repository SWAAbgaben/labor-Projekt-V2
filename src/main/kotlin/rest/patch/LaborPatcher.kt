package com.acme.labor.rest.patch

import com.acme.labor.entity.Labor
import com.acme.labor.entity.TestTyp

/*
 * Copyright (C) 2017 - present Juergen Zimmermann, Hochschule Karlsruhe
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * Singleton-Klasse, um PATCH-Operationen auf Kunde-Objekte anzuwenden.
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 */
object LaborPatcher {
    /**
     * PATCH-Operationen werden auf ein Kunde-Objekt angewandt.
     * @param labor Das zu modifizierende entity.Labor-Objekt.
     * @param operations Die anzuwendenden Operationen.
     * @return Ein entity.Labor-Objekt mit den modifizierten Properties.
     */
    fun patch(labor: Labor, operations: List<PatchOperation>): Labor {
        val replaceOps = operations.filter { "replace" == it.op }
        var laborUpdated = replaceOps(labor, replaceOps)

        val addOps = operations.filter { "add" == it.op }
        laborUpdated = addLaborTests(laborUpdated, addOps)

        val removeOps = operations.filter { "remove" == it.op }
        return removeLaborTests(laborUpdated, removeOps)
    }

    private fun replaceOps(labor: Labor, ops: Collection<PatchOperation>): Labor {
        var laborUpdated = labor
        ops.forEach { (_, path, value) ->
            when (path) {
                "/name" -> laborUpdated = replaceName(laborUpdated, value)
                "/telefonnummer" -> laborUpdated = replaceTelefonnummer(laborUpdated, value)
            }
        }
        return laborUpdated
    }

    private fun replaceName(labor: Labor, name: String) = labor.copy(name = name)

    private fun replaceTelefonnummer(labor: Labor, telefonnummer: String) = labor.copy(telefonnummer = telefonnummer)

    private fun addLaborTests(labor: Labor, ops: Collection<PatchOperation>) =
        if (ops.isEmpty()) {
            labor
        } else {
            var laborUpdated = labor
            ops.filter { op -> "/laborTests" == op.path }
                .forEach { op -> laborUpdated = addLaborTest(op, laborUpdated) }
            laborUpdated
        }

    private fun addLaborTest(op: PatchOperation, labor: Labor): Labor {
        val laborTestStr = op.value
        val laborTest = TestTyp.build(laborTestStr) ?: return labor
        val laborTests = labor.laborTests.toMutableList()
        laborTests.add(laborTest)
        return labor.copy(laborTests = laborTests)
    }

    private fun removeLaborTests(labor: Labor, ops: List<PatchOperation>) =
        if (ops.isEmpty()) {
            labor
        } else {
            var laborUpdated = labor
            ops.filter { "/laborTests" == it.path }
                .forEach { laborUpdated = removeLaborTests(it, labor) }
            laborUpdated
        }

    private fun removeLaborTests(op: PatchOperation, labor: Labor): Labor {
        val laborTestStr = op.value
        val laborTest = TestTyp.build(laborTestStr) ?: return labor
        val laborTests = labor.laborTests.filter { it != laborTest }
        return labor.copy(laborTests = laborTests)
    }
}

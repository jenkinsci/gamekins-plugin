/*
 * Copyright 2021 Gamekins contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gamekins.mutation

import java.io.Serializable

/**
 * Represents all information about a mutation that Gamekins needs to build mutation test challenge
 *
 * [result]: can be "killed" or "survived", it is used to determine whether a mutation test challenge was solved or not
 * [uniqueID]: created from hash value of a mutation in MoCo
 *
 * @author Tran Phan
 * @since 0.3
 */

data class MutationInfo (
    val mutationDetails: MutationDetails,
    val result: String,
    val uniqueID: Int,
    var killedByTest: String = "None",
) : Serializable
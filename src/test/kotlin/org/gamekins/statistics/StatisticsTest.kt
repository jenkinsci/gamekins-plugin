/*
 * Copyright 2022 Gamekins contributors
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

package org.gamekins.statistics

import hudson.model.Result
import hudson.tasks.junit.TestResultAction
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.unmockkAll

class StatisticsTest : FeatureSpec({

    afterSpec {
        unmockkAll()
    }

    //Difficult to test since RunList cannot be instantiated and mocking would need different iterators
    xfeature("addRunEntry") {
        val testing = mockkClass(org.jenkinsci.plugins.workflow.job.WorkflowJob::class)
        every { testing.name } returns "testing"
        val run = mockkClass(org.jenkinsci.plugins.workflow.job.WorkflowRun::class)
        every { run.getNumber() } returns 1
        every { run.result } returns Result.SUCCESS
        every { run.startTimeInMillis } returns 0
        every { run.getAction(TestResultAction::class.java) } returns null
        //every { testing.builds } returns RunList(listOf(run))
        val job = mockkClass(org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject::class)
        every { job.items } returns listOf()
        every { job.fullName } returns "test-project"
    }

    feature("isNotFullyInitialized") {
        val job = mockkClass(org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject::class)
        every { job.items } returns listOf()
        every { job.fullName } returns "test-project"

        Statistics(job).isNotFullyInitialized() shouldBe false
    }

    xfeature("printToXML") {
    }
})
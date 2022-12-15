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

package org.gamekins.util

import hudson.FilePath
import hudson.model.TaskListener
import hudson.model.User
import hudson.tasks.Mailer.UserProperty
import org.gamekins.GameUserProperty
import jenkins.security.MasterToSlaveCallable
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevTree
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.treewalk.AbstractTreeIterator
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.eclipse.jgit.treewalk.EmptyTreeIterator
import org.gamekins.file.FileDetails
import org.gamekins.file.SourceFileDetails
import org.gamekins.file.TestFileDetails
import org.gamekins.file.OtherFileDetails
import org.gamekins.util.Constants.Parameters
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.io.Serializable
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.jvm.Throws

/**
 * Util object for interaction with git and JGit.
 *
 * @author Philipp Straubinger
 * @since 0.1
 */
object GitUtil {

    /**
     * Returns the current branch of the git repository in the [workspace].
     */
    @JvmStatic
    fun getBranch(workspace: FilePath): String {
        try {
            return workspace.act(BranchCallable(workspace.remote))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }

    /**
     * Returns the iterator of a commit [commitId] in a [git] repository according to the position in the history tree.
     */
    @JvmStatic
    @Throws(IOException::class)
    private fun getCanonicalTreeParser(git: Git, commitId: ObjectId): AbstractTreeIterator {
        val walk = RevWalk(git.repository)
        val commit = walk.parseCommit(commitId)
        val treeId = commit.tree.id
        val reader = git.repository.newObjectReader()
        return CanonicalTreeParser(null, reader, treeId)
    }

    /**
     * Returns the commit information according to a [hash] in a [repo]sitory.
     */
    @JvmStatic
    @Throws(IOException::class)
    private fun getCommit(repo: Repository, hash: String): RevCommit {
        val walk = RevWalk(repo)
        val id = repo.resolve(hash)
        val commit = walk.parseCommit(id)
        walk.dispose()
        return commit
    }

    /**
     * Returns the difference between the [newCommit] and its previous commit in the [repo]sitory and [git].
     */
    @JvmStatic
    @Throws(IOException::class)
    private fun getDiffOfCommit(git: Git, repo: Repository, newCommit: RevCommit): String {
        val oldCommit = getPrevHash(repo, newCommit)
        val oldTreeIterator = if (oldCommit == null) EmptyTreeIterator() else getCanonicalTreeParser(git, oldCommit)
        val newTreeIterator = getCanonicalTreeParser(git, newCommit)
        val outputStream: OutputStream = ByteArrayOutputStream()
        val formatter = DiffFormatter(outputStream)

        formatter.setRepository(git.repository)
        formatter.format(oldTreeIterator, newTreeIterator)
        return outputStream.toString()
    }

    /**
     * Returns the head information according to a [repo]sitory.
     */
    @JvmStatic
    @Throws(IOException::class)
    fun getHead(repo: Repository): RevCommit {
        return getCommit(repo, Constants.HEAD)
    }

    /**
     * Returns a list of last changed classes. It searches the last [count] of commits in the history
     * (or until a certain [commitHash]) and assigns the files changed in these commits to the according [users].
     * [parameters] are needed for information about the JaCoCo paths and the [listener] reports the events to the
     * console output of Jenkins.
     */
    @JvmStatic
    fun getLastChangedClasses(
        commitHash: String, parameters: Parameters, listener: TaskListener,
        users: ArrayList<GameUser>
    ): List<SourceFileDetails> {

        return parameters.workspace.act(LastChangedFilesCallable(parameters, commitHash, users, listener))
            .filterIsInstance<SourceFileDetails>()
    }

    /**
     * Returns a list of last changed files of a [user]. It searches the last [count] of commits in the history
     * (or until a certain [commitHash]) and assigns the files changed in these commits to the
     * according [users]. [parameters] are needed for information about the JaCoCo paths and the [listener] reports
     * the events to the console output of Jenkins.
     */
    @JvmStatic
    fun getLastChangedClassesOfUser(commitHash: String, parameters: Parameters,
                                    listener: TaskListener, user: GameUser, users: ArrayList<GameUser>,
    ): List<SourceFileDetails> {
        return getLastChangedClasses(commitHash, parameters, listener, users)
            .filter { it.changedByUsers.contains(user) }
    }

    /**
     * Returns a list of last changed files. It searches the last [count] of commits in the history
     * (or until a certain [commitHash]) and assigns the files changed in these commits to the
     * according [users]. [parameters] are needed for information about the JaCoCo paths and the [listener] reports
     * the events to the console output of Jenkins.
     */
    @JvmStatic
    fun getLastChangedFiles(commitHash: String, parameters: Parameters,
                            users: ArrayList<GameUser>, listener: TaskListener
    ): ArrayList<FileDetails> {

        val builder = FileRepositoryBuilder()
        val repo = builder.setGitDir(File(parameters.remote + "/.git")).setMustExist(true).build()
        val walk = RevWalk(repo)

        val headCommit = getHead(repo)
        val git = Git(repo)

        var targetCommit: RevCommit? = null
        if (commitHash.isNotEmpty()) {
            targetCommit = getCommit(repo, commitHash)
        }

        if (targetCommit == headCommit) return arrayListOf()

        var totalCount = 0
        var currentCommits = hashSetOf<RevCommit>()
        val searchedCommits = hashSetOf<RevCommit>()
        currentCommits.add(headCommit)
        if (targetCommit != null) {
            searchedCommits.add(targetCommit)
            searchedCommits.addAll(targetCommit.parents)

            targetCommit.parents.filter { it.parents != null }.forEach { searchedCommits.addAll(it.parents) }
        }
        val nearToLeaf = ((targetCommit?.parents?.size ?: -2) + 1) == searchedCommits.size

        val files = arrayListOf<FileDetails>()
        val authorMapping = HashMap<PersonIdent, GameUser>()

        while (totalCount < parameters.searchCommitCount) {
            listener.logger.println("[Gamekins] Searched through $totalCount Commits")
            if (currentCommits.isEmpty()) break

            val newCommits = HashSet<RevCommit>()
            for (commit in currentCommits) {
                searchedCommits.add(commit)

                //Add the next commits if they have not been analysed before
                for (parent in commit.parents) {
                    if (!searchedCommits.contains(parent) && !newCommits.contains(parent)
                        && !currentCommits.contains(parent)
                    ) {
                        newCommits.add(walk.parseCommit(repo.resolve(parent.name)))
                    }
                    //Reset the tree
                    walk.dispose()
                }

                //Not interested in merge commits
                if (commit.shortMessage.lowercase(Locale.getDefault()).contains("merge")) continue

                val diff = getDiffOfCommit(git, repo, commit)
                val lines = diff.split("\n".toRegex())
                for (i in lines.indices) {
                    val line = lines[i]

                    if (i + 1 < lines.size
                        && (line.contains("diff --git") && !lines[i + 1].contains("deleted"))
                        && line.split(" ".toRegex())[2].isNotEmpty()
                    ) {
                        val path = line.split(" ".toRegex())[2].substring(1)
                        val pathSplit = path.split("/".toRegex())

                        //Map the author of the commit to the Jenkins user
                        var user = authorMapping[commit.authorIdent]
                        if (user == null) {
                            user = mapGameUser(commit.authorIdent, users)
                            if (user != null) authorMapping[commit.authorIdent] = user
                        }

                        //Check whether the class was found before
                        val fileName = pathSplit[pathSplit.size - 1].split("\\.".toRegex())[0]
                        val packageName = JacocoUtil.computePackageName(path)
                        var found = false
                        for (details in files) {
                            if (details.fileName == fileName && details.packageName == packageName) {
                                found = true
                                if (user != null) details.addUser(user)
                                break
                            }
                        }

                        //Add a new class
                        if (!found && user != null) {
                            val details: FileDetails = when {
                                pathSplit.contains("test") -> {
                                    TestFileDetails(parameters, path, listener)
                                }
                                //TODO: Fix src in path
                                path.contains("src/") && (path.endsWith(".java") || path.endsWith(".kt")) -> {
                                    SourceFileDetails(parameters, path, listener)
                                }
                                else -> {
                                    OtherFileDetails(parameters, path)
                                }
                            }
                            details.addUser(user)
                            files.add(details)
                        }
                    }
                }

                totalCount++
            }
            if (nearToLeaf && newCommits.contains(targetCommit)) break
            currentCommits = HashSet(newCommits)
        }
        return files
    }

    /**
     * Returns a list of last changed source and test files. It searches the last [count] of commits in the history
     * (or until a certain [commitHash]) and assigns the files changed in these commits to the
     * according [users]. [parameters] are needed for information about the JaCoCo paths and the [listener] reports
     * the events to the console output of Jenkins.
     */
    fun getLastChangedSourceAndTestFiles(commitHash: String, parameters: Parameters,
                                         listener: TaskListener, users: ArrayList<GameUser>
    ): List<FileDetails> {
        return parameters.workspace.act(LastChangedFilesCallable(parameters, commitHash, users, listener))
            .filter{ it is SourceFileDetails || it is TestFileDetails }
    }

    /**
     * Returns a list of last changed tests. It searches the last [count] of commits in the history
     * (or until a certain [commitHash]) and assigns the files changed in these commits to the
     * according [users]. [parameters] are needed for information about the JaCoCo paths and the [listener] reports
     * the events to the console output of Jenkins.
     */
    @JvmStatic
    fun getLastChangedTests(
        commitHash: String, parameters: Parameters, listener: TaskListener, users: ArrayList<GameUser>
    ): List<TestFileDetails> {

        return parameters.workspace.act(LastChangedFilesCallable(parameters, commitHash, users, listener))
            .filterIsInstance<TestFileDetails>()
    }

    /**
     * Returns a list of last changed tests of a [user]. It searches the last [count] of commits in the history
     * (or until a certain [commitHash]) and assigns the files changed in these commits to the
     * according [users]. [parameters] are needed for information about the JaCoCo paths and the [listener] reports
     * the events to the console output of Jenkins.
     */
    @JvmStatic
    fun getLastChangedTestsOfUser(commitHash: String, parameters: Parameters,
                                  listener: TaskListener, user: GameUser, users: ArrayList<GameUser>
    ): List<TestFileDetails> {
        return getLastChangedTests(commitHash, parameters, listener, users)
            .filter { it.changedByUsers.contains(user) }
    }

    /**
     * Returns the commit information of the previous commit for a given [commit] and [repo]sitory.
     */
    @JvmStatic
    @Throws(IOException::class)
    private fun getPrevHash(repo: Repository, commit: RevCommit): RevCommit? {
        val walk = RevWalk(repo)

        walk.markStart(commit)
        for ((count, rev) in walk.withIndex()) {
            if (count == 1) {
                return rev
            }
        }
        walk.dispose()

        return null
    }

    /**
     * Maps the author [ident] of a commit to their corresponding Jenkins user of [users]. Uses the full name, the git
     * names and the mail address for identification.
     */
    @JvmStatic
    fun mapUser(ident: PersonIdent, users: Collection<User>): User? {
        val split = ident.name.split(" ".toRegex())
        for (user in users) {
            if (!PropertyUtil.realUser(user)) continue
            val property = user.getProperty(GameUserProperty::class.java)
            if (property != null && property.getGitNames().contains(ident.name)
                || user.fullName.contains(split[0]) && user.fullName.contains(split[split.size - 1])
                || (user.getProperty(UserProperty::class.java) != null
                        && ident.emailAddress == user.getProperty(UserProperty::class.java).address)
            ) {
                return user
            }
        }
        return null
    }

    /**
     * Maps the author [ident] of a commit to their corresponding Jenkins user of [users] in the class [GameUser].
     * Uses the full name, the git names and the mail address for identification.
     */
    @JvmStatic
    fun mapGameUser(ident: PersonIdent, users: Collection<GameUser>): GameUser? {
        val split = ident.name.split(" ".toRegex())
        for (user in users) {
            if (user.gitNames.contains(ident.name)
                || user.fullName.contains(split[0]) && user.fullName.contains(split[split.size - 1])
                || ident.emailAddress == user.mail
            ) {
                return user
            }
        }
        return null
    }

    /**
     * Maps Jenkins [users] to their corresponding [GameUser].
     */
    @JvmStatic
    fun mapUsersToGameUsers(users: Collection<User>): ArrayList<GameUser> {
        val gameUsers = ArrayList<GameUser>()
        for (user in users) {
            if (!PropertyUtil.realUser(user)) continue
            gameUsers.add(GameUser(user))
        }
        return gameUsers
    }

    fun getChangedClsSinceLastStoredCommit(workspace: FilePath, targetID: String,
                                           packageName: String): List<String>? {
        val builder = FileRepositoryBuilder()
        val repo = builder.setGitDir(File(workspace.remote + "/.git")).setMustExist(true).build()
        val headCommit: RevCommit = getHead(repo)
        val git = Git(repo)
        val commits = git.log().all().call()
        for (commit in commits) {
            if (targetID == commit.name) {
                return listDiff(repo, git, targetID, headCommit.name, packageName)
            }
        }
        return null
    }

    private fun listDiff(repository: Repository, git: Git, oldCommit: String,
                         newCommit: String, packageName: String
    ): List<String>? {
        val diff = git.diff()
            .setOldTree(prepareTreeParser(repository, oldCommit))
            .setNewTree(prepareTreeParser(repository, newCommit))
            .call()
        val classFiles = diff.map { it.newPath }.filter { it.endsWith(".java") }
        val res = mutableListOf<String>()
        classFiles.toMutableList().removeIf{ path: String -> path.split("/".toRegex()).contains("test") }
        classFiles.map {
            try {
                val startIndex = it.indexOf(packageName.replace(".", "/"), 0)
                if (startIndex > -1) {
                    res.add(it.substring(startIndex, it.length - 5))
                }
            } catch (ex: java.lang.Exception) {
                return null
            }
        }
        return res
    }

    private fun prepareTreeParser(repository: Repository, objectId: String): AbstractTreeIterator {
        // from the commit we can build the tree which allows us to construct the TreeParser
        val revWalk = RevWalk(repository)
        val commit: RevCommit = revWalk.parseCommit(repository.resolve(objectId))
        val tree: RevTree = revWalk.parseTree(commit.tree.id)
        val treeParser = CanonicalTreeParser()
        val reader = repository.newObjectReader()
        treeParser.reset(reader, tree.id)
        reader.close()
        revWalk.dispose()
        return treeParser
    }

    /**
     * Returns the branch of a repository on a remote machine.
     *
     * @author Philipp Straubinger
     * @since 0.1
     */
    private class BranchCallable(private val workspace: String) : MasterToSlaveCallable<String, IOException?>() {

        /**
         * Performs computation and returns the result,
         * or throws some exception.
         */
        override fun call(): String {
            val builder = FileRepositoryBuilder()
            try {
                val repo = builder.setGitDir(File("$workspace/.git")).setMustExist(true).build()
                return repo.branch
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return ""
        }
    }

    /**
     * Returns the head of a repository on a remote machine.
     *
     * @author Philipp Straubinger
     * @since 0.1
     */
    class HeadCommitCallable(private val workspace: String) : MasterToSlaveCallable<RevCommit, IOException?>() {

        /**
         * Performs computation and returns the result,
         * or throws some exception.
         */
        @Throws(IOException::class)
        override fun call(): RevCommit {
            val builder = FileRepositoryBuilder()
            val repo = builder.setGitDir(File("$workspace/.git")).setMustExist(true).build()
            return getHead(repo)
        }
    }

    /**
     * Returns the difference in source classes of a commit from the head commit of a repository on a remote machine.
     *
     * @author Tran Phan
     * @since 0.1
     */
    class DiffFromHeadCallable(
        private val workspace: FilePath, private val targetCommitID: String, private val packageName: String
    ) : MasterToSlaveCallable<List<String>, IOException?>() {
        @Throws(IOException::class)
        override fun call(): List<String>? {
            return getChangedClsSinceLastStoredCommit(workspace, targetCommitID, packageName)
        }
    }

    /**
     * Returns the last changed files of a repository on a remote machine.
     *
     * @author Philipp Straubinger
     * @since 0.1
     */
    private class LastChangedFilesCallable constructor(
        private val parameters: Parameters,
        private val commitHash: String,
        private val users: ArrayList<GameUser>,
        private val listener: TaskListener
    ) : MasterToSlaveCallable<ArrayList<FileDetails>, IOException?>() {

        /**
         * Performs computation and returns the result,
         * or throws some exception.
         */
        override fun call(): ArrayList<FileDetails> {
            return getLastChangedFiles(commitHash, parameters, users, listener)
        }
    }

    /**
     * The internal representation of a [User] in Jenkins since it cannot be serialised and sent to a remote machine.
     *
     * @author Philipp Straubinger
     * @since 0.1
     */
    class GameUser(user: User) : Serializable {

        val id: String = user.id
        val fullName: String = user.fullName
        val mail: String =
            if (user.getProperty(UserProperty::class.java) == null) ""
            else user.getProperty(UserProperty::class.java).address
        val gitNames: HashSet<String> =
            if (user.getProperty(GameUserProperty::class.java) == null) HashSet()
            else HashSet(user.getProperty(GameUserProperty::class.java).getGitNames())

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || javaClass != other.javaClass) return false
            val gameUser = other as GameUser
            return id == gameUser.id && fullName == gameUser.fullName && mail == gameUser.mail
                    && gitNames == gameUser.gitNames
        }

        /**
         * Returns the Jenkins [User] represented by this [GameUser].
         */
        fun getUser(): User? {
            for (user in User.getAll()) {
                if (!PropertyUtil.realUser(user)) continue
                if (user.id == id) return user
            }
            return null
        }

        override fun hashCode(): Int {
            return Objects.hash(id, fullName, mail, gitNames)
        }
    }
}
